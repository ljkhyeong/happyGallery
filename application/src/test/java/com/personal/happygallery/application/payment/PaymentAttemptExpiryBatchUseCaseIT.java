package com.personal.happygallery.application.payment;

import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.application.batch.BatchResult;
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
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired UserStorePort userStore;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentProvider paymentProvider;

    @BeforeEach
    void setUp() {
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
        assertThatThrownBy(() -> confirmUseCase.confirm(new ConfirmCommand(
                "expired-payment-key", expired.orderId(), expired.amount(), auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ATTEMPT_EXPIRED));
        verifyNoInteractions(paymentProvider);
    }

    private PrepareResult preparePass(AuthContext auth) {
        return prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(auth.userId()),
                auth));
    }
}
