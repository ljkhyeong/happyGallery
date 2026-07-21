package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.dashboard.port.in.DashboardQueryUseCase;
import com.personal.happygallery.application.payment.RefundExecutionService;
import com.personal.happygallery.application.payment.port.in.RefundRecoveryUseCase;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UseCaseIT
class RefundExecutionServiceUseCaseIT {

    @Autowired RefundExecutionService refundExecutionService;
    @Autowired RefundRecoveryUseCase refundRecoveryUseCase;
    @Autowired RefundRepository refundRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired Clock clock;
    @Autowired DashboardQueryUseCase dashboardQueryUseCase;
    @MockitoBean PaymentProvider paymentProvider;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    private Order saveMemberOrder(LocalDateTime paidAt) {
        User member = userStorePort.save(new User(
                "refund-owner@test.local", "password-hash", "환불 테스트 회원", "01099998888"));
        return orderRepository.save(
                Order.forMember(member.getId(), 55_000L, paidAt, paidAt.plusHours(24)));
    }

    @DisplayName("외부 트랜잭션이 롤백되면 환불 PG 호출과 이력 생성이 실행되지 않는다")
    @Test
    void skipsRefundCallAndLog_whenOuterTransactionRollsBack() {
        refundRepository.deleteAllInBatch();

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            refundExecutionService.requestOrderRefund(order.getId(), 55_000L, "payment-key");
            throw new RuntimeException("outer rollback");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("outer rollback");

        verify(paymentProvider, after(300).never()).refund(any(), anyLong(), any());
        assertThat(refundRepository.findAll()).isEmpty();
    }

    @DisplayName("PG 환불 호출은 부모 커밋 이후 별도 스레드에서 실행되고 결과만 별도 트랜잭션으로 저장된다")
    @Test
    void requestOrderRefund_callsPaymentAfterCommitOnRefundExecutor() {
        refundRepository.deleteAllInBatch();

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);
        AtomicBoolean transactionActiveDuringPaymentCall = new AtomicBoolean(true);
        AtomicReference<String> paymentCallThreadName = new AtomicReference<>();
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    transactionActiveDuringPaymentCall.set(TransactionSynchronizationManager.isActualTransactionActive());
                    paymentCallThreadName.set(Thread.currentThread().getName());
                    return RefundResult.success("refund-transaction-key");
                });

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Refund result = transactionTemplate.execute(status ->
                refundExecutionService.requestOrderRefund(order.getId(), 55_000L, "payment-key"));

        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var refunds = refundRepository.findAll();
                    assertThat(refunds).hasSize(1);
                    var refund = refunds.getFirst();
                    assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                });

        verify(paymentProvider).refund("payment-key", 55_000L, result.getIdempotencyKey());
        var refunds = refundRepository.findAll();
        var refund = refunds.getFirst();
        LocalDate today = LocalDate.now(clock);
        var revenue = dashboardQueryUseCase.getRevenueBreakdown(today, today);
        var refundStats = dashboardQueryUseCase.getRefundStats(today, today);
        assertSoftly(softly -> {
            softly.assertThat(transactionActiveDuringPaymentCall.get()).isFalse();
            softly.assertThat(paymentCallThreadName.get()).startsWith("refund-");
            softly.assertThat(result).isNotNull();
            softly.assertThat(result.getStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(result.getRefundTransactionKey()).isNull();
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(refund.getRefundTransactionKey()).isEqualTo("refund-transaction-key");
            softly.assertThat(revenue.orderRevenue()).isZero();
            softly.assertThat(revenue.totalRevenue()).isZero();
            softly.assertThat(refundStats.totalRefundCount()).isOne();
            softly.assertThat(refundStats.totalRefundedAmount()).isEqualTo(55_000L);
        });
    }

    @DisplayName("paymentKey가 없으면 PG 호출 없이 FAILED로 저장한다")
    @Test
    void requestOrderRefund_withoutPaymentKey_marksFailedWithoutPaymentCall() {
        refundRepository.deleteAllInBatch();

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Refund result = transactionTemplate.execute(status ->
                refundExecutionService.requestOrderRefund(order.getId(), 55_000L, null));

        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var refunds = refundRepository.findAll();
                    assertThat(refunds).hasSize(1);
                    var refund = refunds.getFirst();
                    assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
                    assertThat(refund.getFailReason()).contains("paymentKey");
                });

        verify(paymentProvider, after(300).never()).refund(any(), anyLong(), any());
        LocalDate today = LocalDate.now(clock);
        var revenue = dashboardQueryUseCase.getRevenueBreakdown(today, today);
        var refundStats = dashboardQueryUseCase.getRefundStats(today, today);
        assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result.getStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(revenue.orderRevenue()).isEqualTo(55_000L);
            softly.assertThat(revenue.totalRevenue()).isEqualTo(55_000L);
            softly.assertThat(refundStats.totalRefundCount()).isZero();
            softly.assertThat(refundStats.totalRefundedAmount()).isZero();
        });
    }

    @DisplayName("복구 배치는 실행되지 못한 환불 요청을 같은 멱등키로 다시 처리한다")
    @Test
    void recoverPendingRefunds_requestedRefund_executesRefund() {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);
        Refund requestedRefund = refundRepository.save(
                Refund.forOrder(order.getId(), 55_000L, "payment-key"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("refund-transaction-key"));

        var result = refundRecoveryUseCase.recoverPendingRefunds();

        Refund recovered = refundRepository.findById(requestedRefund.getId()).orElseThrow();
        verify(paymentProvider).refund(
                "payment-key",
                55_000L,
                requestedRefund.getIdempotencyKey());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(recovered.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(recovered.getAttemptCount()).isEqualTo(1);
            softly.assertThat(recovered.getRefundTransactionKey()).isEqualTo("refund-transaction-key");
            softly.assertThat(recovered.getSucceededAt()).isNotNull();
        });
    }

    @DisplayName("복구 PG 결과를 확인할 수 없으면 대사 상태로 남기고 배치 성공으로 집계하지 않는다")
    @Test
    void recoverPendingRefunds_reconciliationRequired_recordsPartialFailure() {
        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);
        Refund requestedRefund = refundRepository.save(
                Refund.forOrder(order.getId(), 55_000L, "payment-key"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.reconciliationRequired("PG 호출 결과 불명"));

        var result = refundRecoveryUseCase.recoverPendingRefunds();

        Refund recovered = refundRepository.findById(requestedRefund.getId()).orElseThrow();
        var backlog = refundRepository.summarizeUnresolvedBacklog();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isOne();
            softly.assertThat(recovered.getStatus()).isEqualTo(RefundStatus.RECONCILIATION_REQUIRED);
            softly.assertThat(recovered.getNextAttemptAt()).isAfter(LocalDateTime.now(clock));
            softly.assertThat(backlog)
                    .singleElement()
                    .satisfies(summary -> {
                        softly.assertThat(summary.status()).isEqualTo(RefundStatus.RECONCILIATION_REQUIRED);
                        softly.assertThat(summary.count()).isOne();
                        softly.assertThat(summary.oldestActionAt()).isAfter(LocalDateTime.now(clock));
                    });
        });
    }

    @DisplayName("대사 필요 환불은 PG 취소 이력을 조회해 완료된 동일 금액 거래만 성공 처리한다")
    @Test
    void recoverPendingRefunds_reconciliationRequired_looksUpAndCompletesRefund() {
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = saveMemberOrder(now);
        Refund refund = saveReconciliationRequiredRefund(order, now);
        when(paymentProvider.lookupRefund("payment-key", 55_000L))
                .thenReturn(RefundLookupResult.refunded(
                        "payment-key", 55_000L, "refund-transaction-key"));

        var result = refundRecoveryUseCase.recoverPendingRefunds();

        Refund recovered = refundRepository.findById(refund.getId()).orElseThrow();
        verify(paymentProvider).lookupRefund("payment-key", 55_000L);
        verify(paymentProvider, never()).refund(any(), anyLong(), any());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(recovered.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(recovered.getRefundTransactionKey()).isEqualTo("refund-transaction-key");
        });
    }

    @DisplayName("PG 조회에 취소 이력이 없으면 대사 환불을 재호출 가능 상태로만 전환한다")
    @Test
    void recoverPendingRefunds_reconciliationWithoutCancel_marksRetryableWithoutRefundCall() {
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = saveMemberOrder(now);
        Refund refund = saveReconciliationRequiredRefund(order, now);
        when(paymentProvider.lookupRefund("payment-key", 55_000L))
                .thenReturn(RefundLookupResult.notRefunded(
                        "payment-key", "PG에 완료된 환불 이력이 없습니다."));

        var result = refundRecoveryUseCase.recoverPendingRefunds();

        Refund recovered = refundRepository.findById(refund.getId()).orElseThrow();
        verify(paymentProvider).lookupRefund("payment-key", 55_000L);
        verify(paymentProvider, never()).refund(any(), anyLong(), any());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isOne();
            softly.assertThat(recovered.getStatus()).isEqualTo(RefundStatus.RETRYABLE);
            softly.assertThat(recovered.getNextAttemptAt()).isAfter(now);
        });
    }

    @DisplayName("운영자 명시적 재시도는 최초 멱등키로 환불 요청을 다시 실행한다")
    @Test
    void retryRefund_reconciliationRequired_reusesIdempotencyKey() {
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = saveMemberOrder(now);
        Refund refund = saveReconciliationRequiredRefund(order, now);
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("refund-transaction-key"));

        Refund result = refundExecutionService.retryRefund(refund.getId());

        verify(paymentProvider).refund("payment-key", 55_000L, refund.getIdempotencyKey());
        verify(paymentProvider, never()).lookupRefund(any(), anyLong());
        assertSoftly(softly -> {
            softly.assertThat(result.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(result.getRefundTransactionKey()).isEqualTo("refund-transaction-key");
        });
    }

    @DisplayName("완료된 환불을 재시도하면 INVALID_INPUT 예외가 발생한다")
    @Test
    void retry_nonFailedRefund_throwsInvalidInput() {
        refundRepository.deleteAllInBatch();

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);
        Refund succeededRefund = Refund.forOrder(order.getId(), 55_000L, "payment-key");
        String processingToken = succeededRefund.startProcessing(paidAt, paidAt.minusMinutes(1));
        succeededRefund.markSucceeded(processingToken, "refund-transaction-key", LocalDateTime.now(clock));
        Refund savedRefund = refundRepository.save(succeededRefund);

        assertThatThrownBy(() -> refundExecutionService.retryRefund(savedRefund.getId()))
                .isInstanceOfSatisfying(HappyGalleryException.class, e ->
                        assertSoftly(softly -> {
                            softly.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            softly.assertThat(e.getMessage()).contains("조치 필요 상태 환불만");
                        }));
    }

    private Refund saveReconciliationRequiredRefund(Order order, LocalDateTime now) {
        Refund refund = Refund.forOrder(order.getId(), 55_000L, "payment-key");
        String processingToken = refund.startProcessing(now.minusMinutes(2), now.minusMinutes(3));
        refund.markReconciliationRequired(
                processingToken, "PG 호출 결과 불명", now.minusSeconds(1));
        return refundRepository.save(refund);
    }
}
