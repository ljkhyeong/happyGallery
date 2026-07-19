package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.adapter.out.external.payment.PaymentProvider;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UseCaseIT
class PaymentConfirmUseCaseIT {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired RefundPort refundPort;
    @Autowired OrderReaderPort orderReader;
    @Autowired OrderItemPort orderItemPort;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentProvider paymentProvider;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.success(
                        "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("compensation-refund-key"));
    }

    @DisplayName("confirm은 상품 가격이 바뀌어도 prepare 시점 단가로 주문을 저장한다")
    @Test
    void confirm_productPriceChanged_usesPreparedPriceSnapshot() {
        User user = userStorePort.save(new User("payment-confirm@example.com", "hashed", "회원", "01056785678"));
        Product product = productStorePort.save(readyStockProduct("확정 상품", 31_000L));
        inventoryStorePort.save(inventory(product, 5));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        jdbcTemplate.update("UPDATE products SET price = ? WHERE id = ?", 99_000L, product.getId());

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand("payment-key-confirm", prepared.orderId(), prepared.amount(), auth));

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        var order = orderReader.findById(result.domainId()).orElseThrow();
        var orderItems = orderItemPort.findByOrder(order);
        assertSoftly(softly -> {
            softly.assertThat(result.context()).isEqualTo(PaymentContext.ORDER);
            softly.assertThat(prepared.amount()).isEqualTo(31_000L);
            softly.assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(order.getTotalAmount()).isEqualTo(31_000L);
            softly.assertThat(order.getPaymentKey()).isEqualTo("confirmed-payment-key");
            softly.assertThat(orderItems).hasSize(1);
            softly.assertThat(orderItems.getFirst().getUnitPrice()).isEqualTo(31_000L);
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(attempt.getPaymentKey()).isEqualTo("payment-key-confirm");
            softly.assertThat(attempt.getConfirmedPaymentKey()).isEqualTo("confirmed-payment-key");
        });
        verify(paymentProvider).confirm(
                "payment-key-confirm", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("confirm은 결제를 준비한 회원과 다른 회원이면 PG 호출 전에 거부한다")
    @Test
    void confirm_differentMember_rejectsBeforePgCall() {
        User owner = userStorePort.save(
                new User("payment-owner@example.com", "hashed", "준비 회원", "01011110000"));
        User other = userStorePort.save(
                new User("payment-other@example.com", "hashed", "다른 회원", "01022220000"));
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(owner.getId()),
                AuthContext.member(owner.getId())));

        assertThatThrownBy(() -> confirmUseCase.confirm(new ConfirmCommand(
                "payment-key-other", prepared.orderId(), prepared.amount(), AuthContext.member(other.getId()))))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertSoftly(softly -> {
                            softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            softly.assertThat(exception.getMessage()).contains("현재 인증 정보");
                        }));

        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt -> assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING));
        verify(paymentProvider, never()).confirm(any(), any(), anyLong(), any());
    }

    @DisplayName("confirm은 prepare 금액과 다른 금액이면 도메인 저장 전에 거부한다")
    @Test
    void confirm_amountTampered_rejectsBeforeFulfillment() {
        User user = userStorePort.save(new User("payment-tamper@example.com", "hashed", "회원", "01087654321"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        assertThatThrownBy(() -> confirmUseCase.confirm(
                new ConfirmCommand("payment-key-tampered", prepared.orderId(), prepared.amount() - 1, auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, e ->
                        assertSoftly(softly -> {
                            softly.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            softly.assertThat(e.getMessage()).contains("결제 금액");
                        }));

        assertSoftly(softly -> {
            softly.assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                    .hasValueSatisfying(attempt -> softly.assertThat(attempt.getStatus())
                            .isEqualTo(PaymentAttemptStatus.PENDING));
        });
    }

    @DisplayName("PG 확정 실패는 외부 호출 트랜잭션과 분리되어 FAILED 상태로 저장된다")
    @Test
    void confirm_pgFailure_persistsFailedAttemptOutsidePaymentTransaction() {
        User user = userStorePort.save(new User("payment-failure@example.com", "hashed", "회원", "01011112222"));
        Product product = productStorePort.save(readyStockProduct("확정 실패 상품", 41_000L));
        inventoryStorePort.save(inventory(product, 1));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        AtomicBoolean transactionActiveDuringPgCall = new AtomicBoolean(true);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    transactionActiveDuringPgCall.set(
                            TransactionSynchronizationManager.isActualTransactionActive());
                    return PaymentConfirmResult.failure("PG 승인 거절");
                });

        assertThatThrownBy(() -> confirmUseCase.confirm(
                new ConfirmCommand("payment-key-failure", prepared.orderId(), prepared.amount(), auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED));

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(transactionActiveDuringPgCall.get()).isFalse();
            softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            softly.assertThat(attempt.getFailReason()).isEqualTo("PG 승인 거절");
            softly.assertThat(orderReader.findAllByOrderByCreatedAtDesc()).isEmpty();
        });
    }

    @DisplayName("PG 승인 후 도메인 생성이 실패하면 결제 시도 보상 환불을 실행한다")
    @Test
    void confirm_fulfillmentFailure_compensatesApprovedPayment() {
        User user = userStorePort.save(new User("payment-compensation@example.com", "hashed", "회원", "01033334444"));
        Product product = productStorePort.save(readyStockProduct("보상 환불 상품", 52_000L));
        inventoryStorePort.save(inventory(product, 0));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));

        assertThatThrownBy(() -> confirmUseCase.confirm(
                new ConfirmCommand("payment-key-compensation", prepared.orderId(), prepared.amount(), auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVENTORY_NOT_ENOUGH));

        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(25, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
                    var refunds = refundPort.findAll();
                    assertSoftly(softly -> {
                        softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.COMPENSATED);
                        softly.assertThat(refunds).singleElement().satisfies(refund -> {
                            softly.assertThat(refund.getPaymentAttemptId()).isEqualTo(attempt.getId());
                            softly.assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                            softly.assertThat(refund.getPaymentKey()).isEqualTo("confirmed-payment-key");
                        });
                    });
                });

        var refund = refundPort.findAll().getFirst();
        verify(paymentProvider).refund(
                "confirmed-payment-key", prepared.amount(), refund.getIdempotencyKey());
    }

    @DisplayName("동시에 같은 결제를 확정하면 한 요청만 PG 호출과 주문 생성을 수행한다")
    @Test
    void confirm_concurrently_claimsSingleAttempt() throws Exception {
        User user = userStorePort.save(new User("payment-concurrent@example.com", "hashed", "회원", "01055556666"));
        Product product = productStorePort.save(readyStockProduct("동시 확정 상품", 63_000L));
        inventoryStorePort.save(inventory(product, 2));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        ConfirmCommand command = new ConfirmCommand(
                "payment-key-concurrent", prepared.orderId(), prepared.amount(), auth);
        CountDownLatch pgEntered = new CountDownLatch(1);
        CountDownLatch releasePg = new CountDownLatch(1);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    pgEntered.countDown();
                    if (!releasePg.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("PG 호출 해제 대기 시간 초과");
                    }
                    return PaymentConfirmResult.success(
                            "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00");
                });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> confirmUseCase.confirm(command));
            assertThat(pgEntered.await(3, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> confirmUseCase.confirm(command));

            assertThatThrownBy(second::get)
                    .isInstanceOfSatisfying(ExecutionException.class, exception ->
                            assertThat(exception.getCause())
                                    .isInstanceOfSatisfying(HappyGalleryException.class, cause ->
                                            assertThat(cause.getErrorCode())
                                                    .isEqualTo(ErrorCode.PAYMENT_CONFIRM_IN_PROGRESS)));

            releasePg.countDown();
            PaymentConfirmUseCase.ConfirmResult result = first.get(3, TimeUnit.SECONDS);
            assertSoftly(softly -> {
                softly.assertThat(orderReader.findById(result.domainId())).isPresent();
                softly.assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                        .hasValueSatisfying(attempt -> softly.assertThat(attempt.getStatus())
                                .isEqualTo(PaymentAttemptStatus.CONFIRMED));
            });
            verify(paymentProvider, times(1)).confirm(
                    eq("payment-key-concurrent"), eq(prepared.orderId()), eq(prepared.amount()), eq(prepared.orderId()));
        } finally {
            releasePg.countDown();
            executor.shutdownNow();
        }
    }
}
