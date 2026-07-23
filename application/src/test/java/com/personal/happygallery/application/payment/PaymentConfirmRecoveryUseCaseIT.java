package com.personal.happygallery.application.payment;

import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.PgConfirmationRequired;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UseCaseIT
class PaymentConfirmRecoveryUseCaseIT {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmRecoveryUseCase recoveryUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentReconciliationAdminUseCase reconciliationAdminUseCase;
    @Autowired PaymentConfirmClaimTransactionService claimTransactionService;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired RefundRepository refundRepository;
    @Autowired OrderRepository orderReader;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired MeterRegistry meterRegistry;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentProvider paymentProvider;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.success(
                        "confirmed-payment-key", "CARD", "2026-07-19T10:00:00+09:00"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.retryableFailure("PG 환불 일시 실패"));
    }

    @DisplayName("오래된 PROCESSING 결제는 저장된 요청과 같은 멱등키로 자동 확정한다")
    @Test
    void recover_staleProcessing_reconfirmsWithStoredIdempotencyAndCompletes() {
        PreparedPayment prepared = preparePass("recover-processing@example.com", "01010000001");
        PgConfirmationRequired first = beginConfirm(prepared, "processing-payment-key");
        makeProcessingStale(first.attemptId());

        BatchResult result = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(attemptReader.findById(first.attemptId()))
                    .hasValueSatisfying(attempt -> softly.assertThat(attempt.getStatus())
                            .isEqualTo(PaymentAttemptStatus.CONFIRMED));
        });
        verify(paymentProvider).confirm(
                "processing-payment-key", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("자동 confirm의 PG 일시 실패는 RETRYABLE로 남고 다음 배치가 같은 멱등키로 재확인한다")
    @Test
    void recover_retryableFailure_retriesAutomaticallyWithSameIdempotencyKey() {
        PreparedPayment prepared = preparePass("recover-retryable@example.com", "01010000007");
        PgConfirmationRequired first = beginConfirm(prepared, "retryable-payment-key");
        makeProcessingStale(first.attemptId());
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(
                        PaymentConfirmResult.retryableFailure("PG 일시 실패"),
                        PaymentConfirmResult.success(
                                "confirmed-payment-key", "CARD", "2026-07-19T10:00:00+09:00"));

        BatchResult firstRecovery = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(firstRecovery.successCount()).isZero();
            softly.assertThat(firstRecovery.failureCount()).isOne();
            softly.assertThat(statusOf(first.attemptId())).isEqualTo(PaymentAttemptStatus.RETRYABLE);
        });
        makeRecoveryStale(first.attemptId());

        BatchResult secondRecovery = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(secondRecovery.successCount()).isOne();
            softly.assertThat(secondRecovery.failureCount()).isZero();
            softly.assertThat(statusOf(first.attemptId())).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        });
        verify(paymentProvider, times(2)).confirm(
                "retryable-payment-key", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("14일 안전 구간을 지난 PROCESSING 결제는 PG를 호출하지 않고 대사 대상으로 격리한다")
    @Test
    void recover_processingBeyondAutomaticRetryWindow_requiresReconciliationWithoutPgCall() {
        PreparedPayment prepared = preparePass("recover-reconciliation@example.com", "01010000008");
        PgConfirmationRequired first = beginConfirm(prepared, "reconciliation-payment-key");
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ?, processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusDays(15),
                LocalDateTime.now(clock).minusMinutes(2),
                first.attemptId());
        double metricBefore = meterRegistry
                .counter("happygallery.payment.confirm.reconciliation_required")
                .count();

        BatchResult result = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(meterRegistry
                    .counter("happygallery.payment.confirm.reconciliation_required")
                    .count()).isEqualTo(metricBefore + 1D);
            softly.assertThat(attemptReader.findById(first.attemptId()))
                    .hasValueSatisfying(attempt -> {
                        softly.assertThat(attempt.getStatus())
                                .isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
                        softly.assertThat(attempt.getFailReason()).contains("결제 상태 대사");
                    });
        });
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("14일 안전 구간을 지난 PROCESSING 결제는 사용자 재호출도 PG 전에 대사 상태로 저장한다")
    @Test
    void confirm_processingBeyondAutomaticRetryWindow_persistsReconciliationBeforeFailure() {
        PreparedPayment prepared = preparePass("confirm-reconciliation@example.com", "01010000009");
        PgConfirmationRequired first = beginConfirm(prepared, "manual-reconciliation-payment-key");
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ?, processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusDays(15),
                LocalDateTime.now(clock).minusMinutes(2),
                first.attemptId());
        double metricBefore = meterRegistry
                .counter("happygallery.payment.confirm.reconciliation_required")
                .count();
        ConfirmCommand command = ConfirmCommand.customerRequest(
                "manual-reconciliation-payment-key", prepared.orderId(), prepared.amount(),
                prepared.auth(), null);

        assertThatThrownBy(() -> confirmUseCase.confirm(command))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_RECONCILIATION_REQUIRED));

        assertThat(statusOf(first.attemptId())).isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
        assertThat(meterRegistry
                .counter("happygallery.payment.confirm.reconciliation_required")
                .count()).isEqualTo(metricBefore + 1D);
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("대사 대상 결제가 PG에서 승인 완료로 조회되면 저장 요청을 이행하고 확정한다")
    @Test
    void reconcile_approvedAtPg_fulfillsAndConfirmsStoredRequest() {
        PreparedPayment prepared = preparePass("admin-reconciliation@example.com", "01010000012");
        PgConfirmationRequired first = beginConfirm(prepared, "reconciliation-approved-key");
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ?, processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusDays(15),
                LocalDateTime.now(clock).minusMinutes(2),
                first.attemptId());
        recoveryUseCase.recoverIncompleteConfirms();
        when(paymentProvider.lookupByOrderId(prepared.orderId())).thenReturn(
                PaymentLookupResult.approved(
                        "reconciliation-approved-key", prepared.orderId(), prepared.amount()));

        PaymentReconciliationAdminUseCase.ReconciliationResult result =
                reconciliationAdminUseCase.reconcile(first.attemptId());

        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(result.domainId()).isNotNull();
            softly.assertThat(statusOf(first.attemptId())).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(attemptReader.findById(first.attemptId()))
                    .hasValueSatisfying(attempt -> softly.assertThat(attempt.getFulfilledDomainId())
                            .isEqualTo(result.domainId()));
        });
        verify(paymentProvider).lookupByOrderId(prepared.orderId());
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("PG 미승인으로 종결한 대사 결제는 payload 제거 뒤 재확인에도 결제 실패를 반환한다")
    @Test
    void reconcile_notApprovedAtPg_rejectsLaterConfirmWithoutPayload() {
        PreparedPayment prepared = preparePass("reconciliation-failed@example.com", "01010000013");
        PgConfirmationRequired first = beginConfirm(prepared, "reconciliation-failed-key");
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ?, processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusDays(15),
                LocalDateTime.now(clock).minusMinutes(2),
                first.attemptId());
        recoveryUseCase.recoverIncompleteConfirms();
        when(paymentProvider.lookupByOrderId(prepared.orderId())).thenReturn(
                PaymentLookupResult.notApproved(prepared.orderId(), "PG에서 승인 내역을 찾지 못했습니다."));

        reconciliationAdminUseCase.reconcile(first.attemptId());

        assertThat(attemptReader.findById(first.attemptId()))
                .hasValueSatisfying(attempt -> {
                    assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
                    assertThat(attempt.getPayloadEnc()).isNull();
                });
        assertThatThrownBy(() -> confirmUseCase.confirm(ConfirmCommand.customerRequest(
                "reconciliation-failed-key", prepared.orderId(), prepared.amount(),
                prepared.auth(), null)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED));
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("14일이 지난 0원 PROCESSING 결제는 PG 대사 없이 내부 처리를 재개한다")
    @Test
    void recover_oldZeroAmountProcessing_resumesWithoutReconciliationOrPgCall() {
        User user = userStorePort.save(new User(
                "recover-zero-amount@example.com", "hashed", "회원", "01010000010"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(user.getId(), null, null, null, 999_991L, 999_992L, null),
                auth));
        claimTransactionService.resolveConfirmationStep(
                ConfirmCommand.customerRequest(
                        null, prepared.orderId(), prepared.amount(), auth, prepared.statusToken()));
        Long attemptId = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow().getId();
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ?, processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusDays(15),
                LocalDateTime.now(clock).minusMinutes(2),
                attemptId);

        BatchResult result = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isOne();
            softly.assertThat(statusOf(attemptId)).isEqualTo(PaymentAttemptStatus.FAILED);
            softly.assertThat(refundRepository.findAll()).isEmpty();
        });
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
        verify(paymentProvider, never()).refund(any(), anyLong(), any());
    }

    @DisplayName("PG 장애로 첫 10건이 RETRYABLE이어도 다음 배치는 뒤의 결제를 처리한다")
    @Test
    void recover_retryableBatchBackoff_doesNotStarveLaterAttempt() {
        List<Long> attemptIds = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            PreparedPayment prepared = preparePass(
                    "recover-fairness-" + index + "@example.com",
                    String.format("0102000%04d", index));
            Long attemptId = beginConfirm(prepared, "fairness-payment-key-" + index).attemptId();
            makeProcessingStale(attemptId);
            attemptIds.add(attemptId);
        }
        AtomicInteger calls = new AtomicInteger();
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> calls.getAndIncrement() < 10
                        ? PaymentConfirmResult.retryableFailure("PG 일시 장애")
                        : PaymentConfirmResult.success(
                                "confirmed-payment-key", "CARD", "2026-07-19T10:00:00+09:00"));

        BatchResult firstRecovery = recoveryUseCase.recoverIncompleteConfirms();
        BatchResult secondRecovery = recoveryUseCase.recoverIncompleteConfirms();
        List<PaymentAttemptStatus> statuses = attemptIds.stream().map(this::statusOf).toList();

        assertSoftly(softly -> {
            softly.assertThat(firstRecovery.successCount()).isZero();
            softly.assertThat(firstRecovery.failureCount()).isEqualTo(10);
            softly.assertThat(secondRecovery.successCount()).isOne();
            softly.assertThat(secondRecovery.failureCount()).isZero();
            softly.assertThat(calls.get()).isEqualTo(11);
            softly.assertThat(statuses.subList(0, 10)).containsOnly(PaymentAttemptStatus.RETRYABLE);
            softly.assertThat(statuses.getLast()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        });
    }

    @DisplayName("저장 payload가 손상되어도 복구 시각을 커밋해 즉시 재시도를 막는다")
    @Test
    void recover_corruptPayload_commitsAttemptTimeAndSkipsImmediateRetry() {
        PreparedPayment prepared = preparePass("recover-corrupt-payload@example.com", "01010000011");
        Long attemptId = beginConfirm(prepared, "corrupt-payload-payment-key").attemptId();
        makeProcessingStale(attemptId);
        jdbcTemplate.update(
                "UPDATE payment_attempt SET payload_enc = ? WHERE id = ?",
                "not-valid-base64", attemptId);
        LocalDateTime attemptedAt = LocalDateTime.now(clock);

        BatchResult firstRecovery = recoveryUseCase.recoverIncompleteConfirms();
        BatchResult immediateRetry = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(firstRecovery.successCount()).isZero();
            softly.assertThat(firstRecovery.failureCount()).isOne();
            softly.assertThat(immediateRetry.successCount()).isZero();
            softly.assertThat(immediateRetry.failureCount()).isZero();
            softly.assertThat(attemptReader.findById(attemptId))
                    .hasValueSatisfying(attempt -> {
                        softly.assertThat(attempt.getStatus())
                                .isEqualTo(PaymentAttemptStatus.PROCESSING);
                        softly.assertThat(attempt.getConfirmRecoveryAttemptedAt())
                                .isEqualTo(attemptedAt);
                    });
        });
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("APPROVED 복구는 승인 시각이 오래된 결제만 PG 재호출 없이 완료한다")
    @Test
    void recover_onlyStaleApproved_skipsFreshAttemptsWithoutPgCall() {
        PreparedPayment stale = preparePass("recover-approved-stale@example.com", "01010000002");
        Long staleAttemptId = approve(stale, "stale-payment-key");
        makeApprovalStale(staleAttemptId);

        PreparedPayment freshApproved = preparePass("recover-approved-fresh@example.com", "01010000003");
        Long freshApprovedId = approve(freshApproved, "fresh-approved-payment-key");
        jdbcTemplate.update(
                "UPDATE payment_attempt SET processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusMinutes(10), freshApprovedId);

        PreparedPayment freshProcessing = preparePass("recover-processing-fresh@example.com", "01010000004");
        Long freshProcessingId = beginConfirm(freshProcessing, "fresh-processing-payment-key").attemptId();

        BatchResult result = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(statusOf(staleAttemptId)).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(statusOf(freshApprovedId)).isEqualTo(PaymentAttemptStatus.APPROVED);
            softly.assertThat(statusOf(freshProcessingId)).isEqualTo(PaymentAttemptStatus.PROCESSING);
        });
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("한 APPROVED 복구가 다시 실패해도 보상 요청을 저장하고 다음 결제를 계속 처리한다")
    @Test
    void recover_approvedFailure_persistsCompensationAndContinuesNextAttempt() {
        User orderUser = userStorePort.save(new User(
                "recover-compensation@example.com", "hashed", "회원", "01010000005"));
        Product product = productStorePort.save(readyStockProduct("복구 보상 상품", 52_000L));
        var availableInventory = inventoryStorePort.save(inventory(product, 1));
        AuthContext orderAuth = AuthContext.member(orderUser.getId());
        PaymentPrepareUseCase.PrepareResult orderPrepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        orderUser.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1))),
                orderAuth));
        availableInventory.deduct(1);
        inventoryStorePort.save(availableInventory);
        Long failedAttemptId = approve(
                new PreparedPayment(orderPrepared.orderId(), orderPrepared.amount(), orderAuth),
                "compensation-payment-key");
        makeApprovalStale(failedAttemptId);

        PreparedPayment succeeding = preparePass("recover-continue@example.com", "01010000006");
        Long succeedingAttemptId = approve(succeeding, "succeeding-payment-key");
        makeApprovalStale(succeedingAttemptId);

        BatchResult result = recoveryUseCase.recoverIncompleteConfirms();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isOne();
            softly.assertThat(statusOf(succeedingAttemptId)).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(orderReader.count()).isZero();
        });
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(refundRepository.findAll())
                        .singleElement()
                        .satisfies(refund -> assertSoftly(softly -> {
                            softly.assertThat(refund.getPaymentAttemptId()).isEqualTo(failedAttemptId);
                            softly.assertThat(refund.getPaymentKey()).isEqualTo("confirmed-payment-key");
                            softly.assertThat(refund.getAmount()).isEqualTo(orderPrepared.amount());
                            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.RETRYABLE);
                        })));
        assertThat(statusOf(failedAttemptId)).isEqualTo(PaymentAttemptStatus.COMPENSATION_REQUESTED);
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    private PreparedPayment preparePass(String email, String phone) {
        User user = userStorePort.save(new User(email, "hashed", "회원", phone));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS, new PassPayload(user.getId()), auth));
        return new PreparedPayment(prepared.orderId(), prepared.amount(), auth);
    }

    private PgConfirmationRequired beginConfirm(PreparedPayment prepared, String paymentKey) {
        return (PgConfirmationRequired) claimTransactionService.resolveConfirmationStep(
                ConfirmCommand.customerRequest(
                        paymentKey, prepared.orderId(), prepared.amount(), prepared.auth(), null));
    }

    private Long approve(PreparedPayment prepared, String paymentKey) {
        PgConfirmationRequired required = beginConfirm(prepared, paymentKey);
        assertThat(claimTransactionService.tryMarkApproved(
                required.attemptId(), required.processingToken(), "confirmed-payment-key")).isTrue();
        return required.attemptId();
    }

    private void makeProcessingStale(Long attemptId) {
        jdbcTemplate.update(
                "UPDATE payment_attempt SET processing_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusMinutes(2), attemptId);
    }

    private void makeApprovalStale(Long attemptId) {
        jdbcTemplate.update(
                "UPDATE payment_attempt SET confirmed_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusMinutes(2), attemptId);
    }

    private void makeRecoveryStale(Long attemptId) {
        jdbcTemplate.update(
                "UPDATE payment_attempt SET processing_at = ?, confirm_recovery_attempted_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusMinutes(2),
                LocalDateTime.now(clock).minusMinutes(2),
                attemptId);
    }

    private PaymentAttemptStatus statusOf(Long attemptId) {
        return attemptReader.findById(attemptId).orElseThrow().getStatus();
    }

    private record PreparedPayment(String orderId, long amount, AuthContext auth) {}
}
