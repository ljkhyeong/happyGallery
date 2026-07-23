package com.personal.happygallery.application.payment;

import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.batch.PersonalDataRetentionBatchUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentAttemptExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.token.GuestTokenProperties;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.verifyNoInteractions;

@UseCaseIT
class PaymentAttemptExpiryBatchUseCaseIT {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentAttemptExpiryBatchUseCase expiryUseCase;
    @Autowired PersonalDataRetentionBatchUseCase retentionUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired PaymentAttemptStorePort attemptStore;
    @Autowired PhoneVerificationStorePort phoneVerificationStore;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired UserStorePort userStore;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired GuestTokenProperties guestTokenProperties;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentProvider paymentProvider;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearPassData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("30분이 지난 PENDING 결제는 payload를 제거하고 만료시키며 새 결제는 유지한다")
    @Test
    void expirePendingAttempts_expiresOnlyStaleAttemptsAndRejectsConfirm() {
        User user = userStore.save(new User(
                "payment-expiry@example.com", "hashed", "회원", "01012345678"));
        AuthContext auth = AuthContext.member(user.getId());
        PrepareResult expired = preparePass(auth);
        PrepareResult fresh = preparePass(auth);
        PaymentAttempt expiredAttempt = attemptReader.findByOrderIdExternal(expired.orderId()).orElseThrow();
        PaymentAttempt freshAttempt = attemptReader.findByOrderIdExternal(fresh.orderId()).orElseThrow();
        LocalDateTime expirationBoundary = LocalDateTime.ofInstant(
                clock.instant().minus(DefaultPaymentAttemptExpiryBatchService.PREPARE_TTL), ZoneOffset.UTC);
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ? WHERE id = ?",
                expirationBoundary.minusSeconds(1),
                expiredAttempt.getId());
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ? WHERE id = ?",
                expirationBoundary.plusSeconds(1),
                freshAttempt.getId());

        BatchResult result = expiryUseCase.expirePendingAttempts();

        PaymentAttempt canceled = attemptReader.findById(expiredAttempt.getId()).orElseThrow();
        PaymentAttempt pending = attemptReader.findByOrderIdExternal(fresh.orderId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(canceled.getStatus()).isEqualTo(PaymentAttemptStatus.CANCELED);
            softly.assertThat(canceled.getPayloadEnc()).isNull();
            softly.assertThat(pending.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
            softly.assertThat(pending.getPayloadEnc()).isNotBlank();
        });
        assertThatThrownBy(() -> confirmUseCase.confirm(ConfirmCommand.customerRequest(
                "expired-payment-key", expired.orderId(), expired.amount(), auth, null)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ATTEMPT_EXPIRED));
        verifyNoInteractions(paymentProvider);
    }

    @DisplayName("보존 기간이 지난 최종 결제 암호문과 만료된 휴대폰 인증만 정리한다")
    @Test
    void cleanUpExpiredSensitiveData_preservesRecoverableRecords() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime paymentCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(guestTokenProperties.accessExpiry()),
                ZoneOffset.UTC);
        PaymentAttempt oldConfirmed = saveAttempt("old-payload");
        PaymentAttempt oldReconciliationRequired = saveAttempt("recovery-payload");
        PaymentAttempt freshConfirmed = saveAttempt("fresh-payload");
        markAttempt(oldConfirmed, "CONFIRMED", paymentCutoff.minusSeconds(1), "old-token");
        markAttempt(oldReconciliationRequired, "RECONCILIATION_REQUIRED",
                paymentCutoff.minusSeconds(1), null);
        markAttempt(freshConfirmed, "CONFIRMED", paymentCutoff.plusSeconds(1), "fresh-token");

        saveDeliveredVerification("01011112222", "123456", now.minusDays(2));
        saveDeliveredVerification("01033334444", "654321", now.minusHours(12));
        User cartUser = userStore.save(new User(
                "retention-cart@example.com", "hashed", "장바구니 회원", "01055556666"));
        UUID expiredMergeKey = insertCartMergeRequest(cartUser.getId(), now.minusDays(8));
        UUID retainedMergeKey = insertCartMergeRequest(cartUser.getId(), now.minusDays(6));

        BatchResult result = retentionUseCase.cleanUpExpiredSensitiveData();

        PaymentAttempt cleaned = attemptReader.findById(oldConfirmed.getId()).orElseThrow();
        PaymentAttempt recoverable = attemptReader.findById(oldReconciliationRequired.getId()).orElseThrow();
        PaymentAttempt fresh = attemptReader.findById(freshConfirmed.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(3);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(cleaned.getPayloadEnc()).isNull();
            softly.assertThat(cleaned.getFulfilledAccessTokenEnc()).isNull();
            softly.assertThat(recoverable.getPayloadEnc()).isEqualTo("recovery-payload");
            softly.assertThat(fresh.getPayloadEnc()).isEqualTo("fresh-payload");
            softly.assertThat(fresh.getFulfilledAccessTokenEnc()).isEqualTo("fresh-token");
            softly.assertThat(phoneVerificationReader.findLatestUnverifiedCode("01011112222")).isEmpty();
            softly.assertThat(phoneVerificationReader.findLatestUnverifiedCode("01033334444")).isPresent();
            softly.assertThat(countCartMergeRequests(cartUser.getId())).isOne();
            softly.assertThat(cartMergeRequestExists(cartUser.getId(), expiredMergeKey)).isFalse();
            softly.assertThat(cartMergeRequestExists(cartUser.getId(), retainedMergeKey)).isTrue();
        });
    }

    private UUID insertCartMergeRequest(Long userId, LocalDateTime createdAt) {
        UUID idempotencyKey = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO cart_merge_requests (user_id, idempotency_key, payload_hash, created_at)
                VALUES (?, ?, ?, ?)
                """, userId, idempotencyKey.toString(), "a".repeat(64), createdAt);
        return idempotencyKey;
    }

    private long countCartMergeRequests(Long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cart_merge_requests WHERE user_id = ?", Long.class, userId);
    }

    private boolean cartMergeRequestExists(Long userId, UUID idempotencyKey) {
        return jdbcTemplate.queryForObject("""
                SELECT EXISTS(
                    SELECT 1 FROM cart_merge_requests
                    WHERE user_id = ? AND idempotency_key = ?
                )
                """, Boolean.class, userId, idempotencyKey.toString());
    }

    private PaymentAttempt saveAttempt(String payload) {
        return attemptStore.save(PaymentAttempt.startForMember(
                UUID.randomUUID().toString(), PaymentContext.PASS, 1_000L, payload, 1L));
    }

    private void saveDeliveredVerification(String phone, String code, LocalDateTime expiresAt) {
        PhoneVerification verification = new PhoneVerification(phone, code, expiresAt);
        verification.markDelivered();
        phoneVerificationStore.save(verification);
    }

    private void markAttempt(PaymentAttempt attempt,
                             String status,
                             LocalDateTime createdAt,
                             String accessToken) {
        jdbcTemplate.update("""
                UPDATE payment_attempt
                SET status = ?, created_at = ?, fulfilled_domain_id = 1,
                    fulfilled_access_token_enc = ?
                WHERE id = ?
                """, status, createdAt, accessToken, attempt.getId());
    }

    private PrepareResult preparePass(AuthContext auth) {
        return prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(auth.userId()),
                auth));
    }
}
