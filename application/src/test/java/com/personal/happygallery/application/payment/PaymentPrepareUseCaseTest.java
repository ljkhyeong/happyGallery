package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.PassPriceProperties;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.adapter.out.persistence.policy.PolicyConsentRepository;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PassCreditInsufficientException;
import com.personal.happygallery.domain.error.PassExpiredException;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.policy.PolicyConsentType;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

@UseCaseIT
class PaymentPrepareUseCaseTest {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;
    @Autowired PolicyConsentRepository policyConsentRepository;
    @Autowired PaymentStatusQueryUseCase statusQueryUseCase;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired CartUseCase cartUseCase;
    @Autowired CartItemStorePort cartItemStorePort;
    @Autowired PhoneVerificationReaderPort phoneVerificationReaderPort;
    @Autowired PhoneVerificationStorePort phoneVerificationStorePort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired PassPriceProperties passPriceProperties;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PhoneVerificationAttemptGuard phoneVerificationAttemptGuard;
    @MockitoSpyBean PolicyConsentService policyConsentService;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearCartData();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("prepare는 주문 예약 8회권 금액을 서버 기준으로 확정하고 결제 시도를 저장한다")
    @Test
    void prepare_calculatesServerOwnedAmountsAndStoresAttempts() {
        User user = userStorePort.save(new User("payment-prepare@example.com", "hashed", "회원", "01012341234"));
        Product product = productStorePort.save(readyStockProduct("서버 가격 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 10));
        BookingClass cls = classStorePort.save(bookingClass("금액 클래스", "CRAFT", 120, 50_000L, 30));
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult order = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(new OrderItemRef(product.getId(), 2))),
                auth));
        PaymentPrepareUseCase.PrepareResult booking = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(user.getId(), null, null, null, slot.getId(), null, DepositPaymentMethod.CARD),
                auth));
        PaymentPrepareUseCase.PrepareResult pass = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        assertSoftly(softly -> {
            softly.assertThat(order.amount()).isEqualTo(58_000L);
            softly.assertThat(order.statusToken()).isNull();
            softly.assertThat(booking.amount()).isEqualTo(5_000L);
            softly.assertThat(pass.amount()).isEqualTo(passPriceProperties.totalPrice());
            softly.assertThat(attemptReader.findByOrderIdExternal(order.orderId()))
                    .hasValueSatisfying(attempt -> {
                        softly.assertThat(attempt.getContext()).isEqualTo(PaymentContext.ORDER);
                        softly.assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
                    });
        });
    }

    @DisplayName("진행 중인 회원 결제 준비가 있으면 회원 탈퇴를 거절한다")
    @Test
    void prepare_pendingMemberPaymentBlocksWithdrawal() {
        User user = userStorePort.save(new User(
                "withdraw-payment@example.com", "hashed", "회원", "01011112222"));
        AuthContext auth = AuthContext.member(user.getId());
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.PASS,
                new PassPayload(user.getId()),
                auth));

        assertThatThrownBy(() -> accountLifecycleUseCase.withdraw(new WithdrawCommand(
                user.getId(), user.getCredentialVersion(), true)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWAL_BLOCKED));
        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt ->
                        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING));
    }

    @DisplayName("비회원 prepare는 트랜잭션 밖에서 시도 제한을 확인하고 인증 코드를 한 번 소비한다")
    @Test
    void prepare_guestIssuesPaymentStatusToken() {
        Product product = productStorePort.save(readyStockProduct("비회원 결제 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 1));
        saveVerification("01012341234", "123456");
        AtomicBoolean transactionActiveDuringAttemptGuard = new AtomicBoolean(true);
        doAnswer(invocation -> {
            transactionActiveDuringAttemptGuard.set(
                    TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(phoneVerificationAttemptGuard).check("01012341234");

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                guestOrderPayload(product.getId()),
                AuthContext.guest()));

        var status = statusQueryUseCase.getStatus(
                prepared.orderId(), AuthContext.guest(), prepared.statusToken());
        assertSoftly(softly -> {
            softly.assertThat(prepared.statusToken()).isNotBlank();
            softly.assertThat(status.status())
                    .isEqualTo(PaymentStatusQueryUseCase.CustomerPaymentStatus.READY);
            softly.assertThat(status.amount()).isEqualTo(29_000L);
            softly.assertThat(transactionActiveDuringAttemptGuard).isFalse();
            Long attemptId = attemptReader.findByOrderIdExternal(prepared.orderId())
                    .orElseThrow()
                    .getId();
            softly.assertThat(policyConsentRepository.findByPaymentAttemptIdOrderById(attemptId))
                    .extracting(
                            consent -> consent.getType(),
                            consent -> consent.getPurpose(),
                            consent -> consent.getPolicyVersion())
                    .containsExactly(
                            tuple(
                                    PolicyConsentType.TERMS_OF_SERVICE,
                                    PolicyConsentPurpose.GUEST_ORDER_PAYMENT,
                                    "2026-08-08-v1"),
                            tuple(
                                    PolicyConsentType.PRIVACY_POLICY,
                                    PolicyConsentPurpose.GUEST_ORDER_PAYMENT,
                                    "2026-08-11-v1"));
        });
        assertThatThrownBy(() -> statusQueryUseCase.getStatus(
                prepared.orderId(), AuthContext.guest(), "wrong-token"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                guestOrderPayload(product.getId()),
                AuthContext.guest())))
                .isInstanceOf(PhoneVerificationFailedException.class);
    }

    @DisplayName("비회원 주문에 회원 ID를 보내면 인증·동의·결제 시도 부작용 없이 거절한다")
    @Test
    void prepare_guestOrderWithUserId_rejectedBeforeSideEffects() {
        Product product = productStorePort.save(readyStockProduct("비회원 주체 검증 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 1));
        saveVerification("01012341234", "123456");
        OrderPayload payload = new OrderPayload(
                999L,
                "01012341234",
                "123456",
                "비회원",
                List.of(new OrderItemRef(product.getId(), 1)),
                false,
                FulfillmentType.PICKUP,
                null,
                null,
                false,
                acceptedPolicies());

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER, payload, AuthContext.guest())))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertSoftly(softly -> {
            softly.assertThat(paymentAttemptRepository.count()).isZero();
            softly.assertThat(policyConsentRepository.count()).isZero();
            softly.assertThat(phoneVerificationReaderPort.findLatestUnverifiedCode(
                    "01012341234", PhoneVerificationPurpose.GUEST_ORDER)).isPresent();
        });
        verifyNoInteractions(phoneVerificationAttemptGuard);
    }

    @DisplayName("비회원 prepare 저장이 실패하면 인증 코드 소비도 롤백되어 재시도할 수 있다")
    @Test
    void prepare_guestPersistenceFailure_rollsBackVerificationConsumption() {
        Product product = productStorePort.save(readyStockProduct("비회원 롤백 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 1));
        saveVerification("01012341234", "123456");
        PrepareCommand command = new PrepareCommand(
                PaymentContext.ORDER,
                guestOrderPayload(product.getId()),
                AuthContext.guest());
        PolicyConsentService policyConsentTarget =
                AopTestUtils.getTargetObject(policyConsentService);
        doThrow(new IllegalStateException("동의 이력 저장 실패"))
                .doCallRealMethod()
                .when(policyConsentTarget)
                .recordForPaymentAttempt(
                        anyLong(),
                        eq(PolicyConsentPurpose.GUEST_ORDER_PAYMENT),
                        any());

        assertThatThrownBy(() -> prepareUseCase.prepare(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("동의 이력 저장 실패");
        assertSoftly(softly -> {
            softly.assertThat(paymentAttemptRepository.count()).isZero();
            softly.assertThat(policyConsentRepository.count()).isZero();
        });

        PaymentPrepareUseCase.PrepareResult retried = prepareUseCase.prepare(command);

        assertSoftly(softly -> {
            softly.assertThat(retried.statusToken()).isNotBlank();
            softly.assertThat(paymentAttemptRepository.count()).isEqualTo(1);
            softly.assertThat(policyConsentRepository.count()).isEqualTo(2);
        });
    }

    @DisplayName("직접 주문 prepare는 판매 중지 상품을 결제 대상으로 확정하지 않는다")
    @Test
    void prepare_inactiveDirectOrder_rejected() {
        User user = userStorePort.save(new User(
                "inactive-order@example.com", "hashed", "회원", "01055556666"));
        Product product = productStorePort.save(readyStockProduct("판매 중지 상품", 29_000L));
        product.deactivate();
        productStorePort.save(product);
        inventoryStorePort.save(inventory(product, 1));

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 1))),
                AuthContext.member(user.getId()))))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("판매 중인 상품");
    }

    @DisplayName("장바구니 결제 prepare는 화면 조회 뒤 수량이 바뀐 오래된 스냅샷을 거절한다")
    @Test
    void prepare_cartCheckoutRejectsStaleSnapshot() {
        User user = userStorePort.save(new User(
                "cart-snapshot@example.com", "hashed", "장바구니 회원", "01012123434"));
        Product product = productStorePort.save(readyStockProduct("스냅샷 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 5));
        cartItemStorePort.save(new CartItem(
                user.getId(), product.getId(), 1, LocalDateTime.now(clock)));
        String staleVersion = cartUseCase.getCart(user.getId()).cartVersion();
        cartUseCase.updateItemQty(user.getId(), product.getId(), 2);

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                cartOrderPayload(user.getId(), staleVersion),
                AuthContext.member(user.getId()))))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(
                                ErrorCode.CART_SNAPSHOT_CHANGED));
        assertThat(paymentAttemptRepository.count()).isZero();

        String currentVersion = cartUseCase.getCart(user.getId()).cartVersion();
        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                cartOrderPayload(user.getId(), currentVersion),
                AuthContext.member(user.getId())));

        assertThat(prepared.amount()).isEqualTo(58_000L);
    }

    @DisplayName("주문 prepare는 상품별 수량과 재고 및 안전 결제 금액 상한을 우회할 수 없다")
    @Test
    void prepare_rejectsExcessiveQuantityAndUnsafeAmount() {
        User user = userStorePort.save(new User(
                "order-limit@example.com", "hashed", "회원", "01077778888"));
        Product product = productStorePort.save(readyStockProduct(
                "고액 상품", PaymentAmountPolicy.MAX_AMOUNT));
        inventoryStorePort.save(inventory(product, 99));
        Product lowStockProduct = productStorePort.save(readyStockProduct("재고 부족 상품", 10_000L));
        inventoryStorePort.save(inventory(lowStockProduct, 1));
        AuthContext auth = AuthContext.member(user.getId());

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 100))),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(
                                new OrderItemRef(product.getId(), 50),
                                new OrderItemRef(product.getId(), 50))),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(product.getId(), 2))),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null,
                        List.of(new OrderItemRef(lowStockProduct.getId(), 2))),
                auth)))
                .isInstanceOf(InventoryNotEnoughException.class);
    }

    @DisplayName("예약 prepare는 허용 범위를 벗어난 인원과 다인 8회권 예약을 거절한다")
    @Test
    void prepare_rejectsParticipantCountOutsideBookingPolicy() {
        User user = userStorePort.save(new User(
                "booking-participant-limit@example.com", "hashed", "예약 회원", "01044445555"));
        BookingClass cls = classStorePort.save(
                bookingClass("인원 제한 클래스", "CRAFT", 120, 50_000L, 30));
        Slot slot = slotStorePort.save(slot(cls, FUTURE, FUTURE.plusHours(2)));
        BookingClass expensiveClass = classStorePort.save(
                bookingClass(
                        "고액 클래스", "CRAFT", 120,
                        PaymentAmountPolicy.MAX_AMOUNT, 30));
        Slot expensiveSlot = slotStorePort.save(
                slot(expensiveClass, FUTURE.plusHours(3), FUTURE.plusHours(5)));
        AuthContext auth = AuthContext.member(user.getId());

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, slot.getId(), null,
                        DepositPaymentMethod.CARD, 9, null),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, expensiveSlot.getId(), null,
                        DepositPaymentMethod.CARD, 2, null),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, slot.getId(), 99L,
                        null, 2, null),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @DisplayName("8회권 예약 prepare는 소유권과 현재 사용 가능 여부 및 클래스 적용 정책을 확인한다")
    @Test
    void prepare_validatesPassBeforeCreatingPaymentAttempt() {
        User user = userStorePort.save(new User(
                "pass-prepare@example.com", "hashed", "예약 회원", "01066667777"));
        User otherUser = userStorePort.save(new User(
                "other-pass-owner@example.com", "hashed", "다른 회원", "01088889999"));
        BookingClass craftClass = classStorePort.save(
                bookingClass("회차권 공예 클래스", "CRAFT", 120, 50_000L, 30));
        Slot craftSlot = slotStorePort.save(
                slot(craftClass, FUTURE, FUTURE.plusHours(2)));
        BookingClass perfumeClass = classStorePort.save(
                bookingClass("회차권 제외 향수 클래스", "PERFUME", 120, 50_000L, 30));
        Slot perfumeSlot = slotStorePort.save(
                slot(perfumeClass, FUTURE.plusHours(3), FUTURE.plusHours(5)));
        LocalDateTime now = LocalDateTime.now(clock);
        PassPurchase validPass = passPurchaseStorePort.save(
                passPurchase(user.getId(), now.plusDays(30), 320_000L));
        PassPurchase otherPass = passPurchaseStorePort.save(
                passPurchase(otherUser.getId(), now.plusDays(30), 320_000L));
        PassPurchase expiredPass = passPurchaseStorePort.save(
                passPurchase(user.getId(), now, 320_000L));
        PassPurchase depletedPass =
                passPurchase(user.getId(), now.plusDays(30), 320_000L);
        for (int credit = 0; credit < PassPurchase.TOTAL_CREDITS; credit++) {
            depletedPass.useCredit(now);
        }
        depletedPass = passPurchaseStorePort.save(depletedPass);
        AuthContext auth = AuthContext.member(user.getId());

        PaymentPrepareUseCase.PrepareResult valid = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                passBookingPayload(user.getId(), craftSlot.getId(), validPass.getId()),
                auth));
        assertThat(valid.amount()).isZero();

        assertPassPrepareFailsWith(
                passBookingPayload(user.getId(), craftSlot.getId(), otherPass.getId()),
                auth,
                NotFoundException.class);
        assertPassPrepareFailsWith(
                passBookingPayload(user.getId(), craftSlot.getId(), expiredPass.getId()),
                auth,
                PassExpiredException.class);
        assertPassPrepareFailsWith(
                passBookingPayload(user.getId(), craftSlot.getId(), depletedPass.getId()),
                auth,
                PassCreditInsufficientException.class);
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                passBookingPayload(user.getId(), perfumeSlot.getId(), validPass.getId()),
                auth)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASS_NOT_APPLICABLE));
    }

    @DisplayName("예약 prepare는 일반 결제와 8회권 모두 결제 시도 생성 전에 슬롯 상태를 확인한다")
    @Test
    void prepare_rejectsUnavailableSlotBeforePaymentAttempt() {
        User user = userStorePort.save(new User(
                "unavailable-slot@example.com", "hashed", "예약 회원", "01033334444"));
        BookingClass cls = classStorePort.save(
                bookingClass("비활성 슬롯 클래스", "CRAFT", 120, 50_000L, 30));
        Slot unavailableSlot = slot(cls, FUTURE, FUTURE.plusHours(2));
        unavailableSlot.deactivate();
        unavailableSlot = slotStorePort.save(unavailableSlot);
        Slot fullSlot = slot(cls, FUTURE.plusHours(3), FUTURE.plusHours(5));
        fullSlot.incrementBookedCount(8);
        fullSlot = slotStorePort.save(fullSlot);
        AuthContext auth = AuthContext.member(user.getId());

        Long slotId = unavailableSlot.getId();
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, slotId, null,
                        DepositPaymentMethod.CARD),
                auth)))
                .isInstanceOf(SlotNotAvailableException.class);
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, slotId, 99L, null),
                auth)))
                .isInstanceOf(SlotNotAvailableException.class);

        Long fullSlotId = fullSlot.getId();
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, fullSlotId, 99L, null),
                auth)))
                .isInstanceOf(CapacityExceededException.class);
    }

    @DisplayName("예약 prepare의 역방향 버퍼 충돌 검사는 시작 경계를 포함하고 종료 경계를 제외한다")
    @Test
    void prepare_checksBookedSlotsInsideExactBufferWindow() {
        User user = userStorePort.save(new User(
                "buffer-precheck@example.com", "hashed", "예약 회원", "01022223333"));
        BookingClass cls = classStorePort.save(
                bookingClass("버퍼 확인 클래스", "CRAFT", 120, 50_000L, 30));
        Slot conflictingSource = slotStorePort.save(
                slot(cls, FUTURE, FUTURE.plusHours(2)));
        Slot bookedAtWindowStart = slot(cls, FUTURE.plusHours(2), FUTURE.plusHours(4));
        bookedAtWindowStart.incrementBookedCount();
        slotStorePort.save(bookedAtWindowStart);

        LocalDateTime nextDayStart = FUTURE.plusDays(1);
        Slot boundarySource = slotStorePort.save(
                slot(cls, nextDayStart, nextDayStart.plusHours(2)));
        Slot bookedAtWindowEnd = slot(
                cls,
                nextDayStart.plusHours(2).plusMinutes(30),
                nextDayStart.plusHours(4).plusMinutes(30));
        bookedAtWindowEnd.incrementBookedCount();
        slotStorePort.save(bookedAtWindowEnd);
        AuthContext auth = AuthContext.member(user.getId());

        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, conflictingSource.getId(), null,
                        DepositPaymentMethod.CARD),
                auth)))
                .isInstanceOf(SlotNotAvailableException.class);

        PaymentPrepareUseCase.PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING,
                new BookingPayload(
                        user.getId(), null, null, null, boundarySource.getId(), null,
                        DepositPaymentMethod.CARD),
                auth));

        assertThat(prepared.amount()).isEqualTo(5_000L);
    }

    private void saveVerification(String phone, String code) {
        PhoneVerification verification = new PhoneVerification(
                phone, code, PhoneVerificationPurpose.GUEST_ORDER,
                LocalDateTime.now(clock).plusMinutes(5));
        verification.markDelivered();
        phoneVerificationStorePort.save(verification);
    }

    private BookingPayload passBookingPayload(Long userId, Long slotId, Long passId) {
        return new BookingPayload(
                userId, null, null, null, slotId, passId, null);
    }

    private void assertPassPrepareFailsWith(
            BookingPayload payload,
            AuthContext auth,
            Class<? extends Throwable> exceptionType) {
        assertThatThrownBy(() -> prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.BOOKING, payload, auth)))
                .isInstanceOf(exceptionType);
    }

    private OrderPayload guestOrderPayload(Long productId) {
        return new OrderPayload(
                null,
                "01012341234",
                "123456",
                "비회원",
                List.of(new OrderItemRef(productId, 1)),
                false,
                FulfillmentType.PICKUP,
                null,
                null,
                false,
                acceptedPolicies());
    }

    private OrderPayload cartOrderPayload(Long userId, String expectedCartVersion) {
        return new OrderPayload(
                userId,
                null,
                null,
                null,
                List.of(),
                true,
                FulfillmentType.PICKUP,
                null,
                null,
                false,
                null,
                expectedCartVersion);
    }
}
