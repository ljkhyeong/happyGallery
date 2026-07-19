package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
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
import com.personal.happygallery.adapter.out.persistence.cart.CartItemRepository;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.cart.CartItem;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.slot;
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
    @Autowired BookingReaderPort bookingReaderPort;
    @Autowired PassPurchaseReaderPort passPurchaseReaderPort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired PhoneVerificationStorePort phoneVerificationStorePort;
    @Autowired CartItemStorePort cartItemStorePort;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentProvider paymentProvider;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAllInBatch();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.success(
                        "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00"));
        when(paymentProvider.refund(any(), anyLong(), any()))
                .thenReturn(RefundResult.success("compensation-refund-key"));
    }

    @DisplayName("장바구니 결제는 prepare 시점 항목으로 주문하고 결제 후 추가한 수량은 남긴다")
    @Test
    void confirm_cartCheckout_consumesOnlyPreparedQuantities() {
        User user = userStorePort.save(new User("cart-payment@example.com", "hashed", "회원", "01067896789"));
        Product product = productStorePort.save(readyStockProduct("장바구니 결제 상품", 31_000L));
        inventoryStorePort.save(inventory(product, 5));
        CartItem cartItem = cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 2, LocalDateTime.of(2026, 7, 19, 10, 0)));
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(), true),
                auth));
        cartItem.addQty(1, LocalDateTime.of(2026, 7, 19, 10, 1));
        cartItemStorePort.save(cartItem);

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand("cart-payment-key", prepared.orderId(), prepared.amount(), auth));

        var order = orderReader.findById(result.domainId()).orElseThrow();
        var remainingCartItem = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(62_000L);
            softly.assertThat(order.getTotalAmount()).isEqualTo(62_000L);
            softly.assertThat(remainingCartItem).hasValueSatisfying(item ->
                    softly.assertThat(item.getQty()).isEqualTo(1));
        });
    }

    @DisplayName("장바구니 결제는 prepare 후 같은 상품을 다시 담아도 새 항목을 제거하지 않는다")
    @Test
    void confirm_cartCheckout_preservesRecreatedCartItem() {
        User user = userStorePort.save(new User(
                "cart-recreated@example.com", "hashed", "장바구니 회원", "01078907890"));
        Product product = productStorePort.save(readyStockProduct("다시 담은 상품", 28_000L));
        inventoryStorePort.save(inventory(product, 5));
        CartItem preparedCartItem = cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 1, LocalDateTime.of(2026, 7, 19, 11, 0)));
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(), true),
                auth));
        cartItemRepository.deleteById(preparedCartItem.getId());
        cartItemRepository.flush();
        CartItem recreatedCartItem = cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 1, LocalDateTime.of(2026, 7, 19, 11, 1)));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand("cart-recreated-key", prepared.orderId(), prepared.amount(), auth));

        assertSoftly(softly -> {
            softly.assertThat(orderReader.findById(result.domainId())).isPresent();
            softly.assertThat(cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId()))
                    .hasValueSatisfying(item -> {
                        softly.assertThat(item.getId()).isEqualTo(recreatedCartItem.getId());
                        softly.assertThat(item.getQty()).isEqualTo(1);
                    });
        });
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

    @DisplayName("완료된 비회원 결제를 재호출하면 저장한 결과를 반환하고 PG와 주문 생성을 반복하지 않는다")
    @Test
    void confirm_completedGuestOrder_returnsStoredResultIdempotently() {
        String phone = "01090908080";
        String verificationCode = "654321";
        PhoneVerification verification = new PhoneVerification(
                phone, verificationCode, LocalDateTime.now(clock).plusMinutes(5));
        verification.markDelivered();
        phoneVerificationStorePort.save(verification);
        Product product = productStorePort.save(readyStockProduct("비회원 멱등 주문 상품", 47_000L));
        inventoryStorePort.save(inventory(product, 2));
        AuthContext auth = AuthContext.guest();
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        null, phone, verificationCode, "비회원",
                        List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        ConfirmCommand command = new ConfirmCommand(
                "guest-idempotent-payment-key", prepared.orderId(), prepared.amount(), auth);

        PaymentConfirmUseCase.ConfirmResult first = confirmUseCase.confirm(command);
        PaymentConfirmUseCase.ConfirmResult replay = confirmUseCase.confirm(command);

        var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(replay).isEqualTo(first);
            softly.assertThat(first.accessToken()).isNotBlank();
            softly.assertThat(orderReader.findAllByOrderByCreatedAtDesc()).hasSize(1);
            softly.assertThat(attempt.getFulfilledDomainId()).isEqualTo(first.domainId());
            softly.assertThat(attempt.getFulfilledAccessTokenEnc())
                    .isNotBlank()
                    .doesNotContain(first.accessToken());
        });
        verify(paymentProvider, times(1)).confirm(
                "guest-idempotent-payment-key", prepared.orderId(), prepared.amount(), prepared.orderId());
    }

    @DisplayName("confirm은 클래스 가격이 바뀌어도 prepare 시점 예약금과 잔금으로 예약을 저장한다")
    @Test
    void confirm_bookingClassPriceChanged_usesPreparedPriceSnapshot() {
        User user = userStorePort.save(
                new User("booking-payment-confirm@example.com", "hashed", "예약 회원", "01034563456"));
        BookingClass bookingClass = classStorePort.save(
                bookingClass("예약금 확정 클래스", "CRAFT", 120, 50_000L, 30));
        Slot slot = slotStorePort.save(slot(bookingClass, FUTURE, FUTURE.plusHours(2)));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, slot.getId(), null, DepositPaymentMethod.CARD),
                auth));
        jdbcTemplate.update("UPDATE classes SET price = ? WHERE id = ?", 90_000L, bookingClass.getId());

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand("booking-payment-key", prepared.orderId(), prepared.amount(), auth));

        var booking = bookingReaderPort.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(5_000L);
            softly.assertThat(booking.getDepositAmount()).isEqualTo(5_000L);
            softly.assertThat(booking.getBalanceAmount()).isEqualTo(45_000L);
            softly.assertThat(booking.getPaymentKey()).isEqualTo("confirmed-payment-key");
        });
    }

    @DisplayName("confirm은 prepare에서 확정한 8회권 가격을 구매 내역에 저장한다")
    @Test
    void confirm_passPurchase_usesPreparedPriceSnapshot() {
        User user = userStorePort.save(
                new User("pass-payment-confirm@example.com", "hashed", "이용권 회원", "01045674567"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand("pass-payment-key", prepared.orderId(), prepared.amount(), auth));

        var passPurchase = passPurchaseReaderPort.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(passPurchase.getTotalPrice()).isEqualTo(prepared.amount());
            softly.assertThat(passPurchase.getPaymentKey()).isEqualTo("confirmed-payment-key");
        });
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

    @DisplayName("stale confirm의 늦은 PG 실패는 새 실행권이 완료한 결제 결과를 덮지 않는다")
    @Test
    void confirm_stalePgFailure_returnsLatestCompletedResult() throws Exception {
        User user = userStorePort.save(new User(
                "payment-stale@example.com", "hashed", "회원", "01066667777"));
        Product product = productStorePort.save(readyStockProduct("stale 확정 상품", 64_000L));
        inventoryStorePort.save(inventory(product, 2));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 1))),
                auth));
        ConfirmCommand command = new ConfirmCommand(
                "payment-key-stale", prepared.orderId(), prepared.amount(), auth);
        CountDownLatch firstPgEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstPg = new CountDownLatch(1);
        AtomicInteger pgCalls = new AtomicInteger();
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    if (pgCalls.getAndIncrement() == 0) {
                        firstPgEntered.countDown();
                        if (!releaseFirstPg.await(3, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("첫 PG 호출 해제 대기 시간 초과");
                        }
                        return PaymentConfirmResult.failure("늦은 PG 실패");
                    }
                    return PaymentConfirmResult.success(
                            "confirmed-payment-key", "CARD", "2026-07-12T10:00:00+09:00");
                });
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var staleConfirm = executor.submit(() -> confirmUseCase.confirm(command));
            assertThat(firstPgEntered.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(jdbcTemplate.update(
                    "UPDATE payment_attempt SET processing_at = ? WHERE order_id_external = ?",
                    LocalDateTime.now(clock).minusMinutes(2), prepared.orderId())).isOne();

            var latestConfirm = executor.submit(() -> confirmUseCase.confirm(command));
            PaymentConfirmUseCase.ConfirmResult latestResult = latestConfirm.get(3, TimeUnit.SECONDS);
            releaseFirstPg.countDown();
            PaymentConfirmUseCase.ConfirmResult staleResult = staleConfirm.get(3, TimeUnit.SECONDS);

            var attempt = attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(staleResult).isEqualTo(latestResult);
                softly.assertThat(orderReader.findAllByOrderByCreatedAtDesc()).hasSize(1);
                softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            });
            verify(paymentProvider, times(2)).confirm(
                    eq("payment-key-stale"), eq(prepared.orderId()), eq(prepared.amount()), eq(prepared.orderId()));
        } finally {
            releaseFirstPg.countDown();
            executor.shutdownNow();
        }
    }
}
