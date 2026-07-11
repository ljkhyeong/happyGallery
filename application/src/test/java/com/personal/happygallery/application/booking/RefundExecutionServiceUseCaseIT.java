package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.payment.RefundExecutionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UseCaseIT
class RefundExecutionServiceUseCaseIT {

    @Autowired RefundExecutionService refundExecutionService;
    @Autowired RefundRepository refundRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired Clock clock;
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

        verify(paymentProvider, after(300).never()).refund(any(), anyLong());
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
        when(paymentProvider.refund(any(), anyLong()))
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
                    assertThat(refunds.get(0).getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                });

        verify(paymentProvider).refund("payment-key", 55_000L);
        var refunds = refundRepository.findAll();
        assertSoftly(softly -> {
            softly.assertThat(transactionActiveDuringPaymentCall.get()).isFalse();
            softly.assertThat(paymentCallThreadName.get()).startsWith("refund-");
            softly.assertThat(result).isNotNull();
            softly.assertThat(result.getStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(result.getRefundTransactionKey()).isNull();
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refunds.get(0).getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
            softly.assertThat(refunds.get(0).getRefundTransactionKey()).isEqualTo("refund-transaction-key");
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
                    assertThat(refunds.get(0).getStatus()).isEqualTo(RefundStatus.FAILED);
                    assertThat(refunds.get(0).getFailReason()).contains("paymentKey");
                });

        verify(paymentProvider, after(300).never()).refund(any(), anyLong());
        assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        });
    }

    @DisplayName("FAILED가 아닌 환불을 재시도하면 INVALID_INPUT 예외가 발생한다")
    @Test
    void retry_nonFailedRefund_throwsInvalidInput() {
        refundRepository.deleteAllInBatch();

        LocalDateTime paidAt = LocalDateTime.now(clock);
        Order order = saveMemberOrder(paidAt);
        Refund succeededRefund = Refund.forOrder(order.getId(), 55_000L, "payment-key");
        succeededRefund.markSucceeded("refund-transaction-key");
        Refund savedRefund = refundRepository.save(succeededRefund);

        assertThatThrownBy(() -> refundExecutionService.retryRefund(savedRefund.getId()))
                .isInstanceOfSatisfying(HappyGalleryException.class, e ->
                        assertSoftly(softly -> {
                            softly.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            softly.assertThat(e.getMessage()).contains("FAILED 상태 환불만");
                        }));
    }
}
