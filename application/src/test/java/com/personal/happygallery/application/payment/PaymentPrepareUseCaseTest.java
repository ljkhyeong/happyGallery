package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.PassPriceProperties;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.adapter.out.persistence.policy.PolicyConsentRepository;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.PhoneVerification;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.policy.PolicyConsentType;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.BookingTestHelper.FUTURE;
import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.groups.Tuple.tuple;

@UseCaseIT
class PaymentPrepareUseCaseTest {

    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired PolicyConsentRepository policyConsentRepository;
    @Autowired PaymentStatusQueryUseCase statusQueryUseCase;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired PhoneVerificationStorePort phoneVerificationStorePort;
    @Autowired PassPriceProperties passPriceProperties;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
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

        assertThatThrownBy(() -> accountLifecycleUseCase.withdraw(user.getId()))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.ACCOUNT_WITHDRAWAL_BLOCKED));
        assertThat(attemptReader.findByOrderIdExternal(prepared.orderId()))
                .hasValueSatisfying(attempt ->
                        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING));
    }

    @DisplayName("비회원 prepare는 인증 코드를 한 번 소비하고 결제 상태 토큰을 발급한다")
    @Test
    void prepare_guestIssuesPaymentStatusToken() {
        Product product = productStorePort.save(readyStockProduct("비회원 결제 상품", 29_000L));
        inventoryStorePort.save(inventory(product, 1));
        saveVerification("01012341234", "123456");

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
                                    "2026-07-21-v1"),
                            tuple(
                                    PolicyConsentType.PRIVACY_POLICY,
                                    PolicyConsentPurpose.GUEST_ORDER_PAYMENT,
                                    "2026-07-21-v1"));
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

    private void saveVerification(String phone, String code) {
        PhoneVerification verification = new PhoneVerification(
                phone, code, LocalDateTime.now(clock).plusMinutes(5));
        verification.markDelivered();
        phoneVerificationStorePort.save(verification);
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
}
