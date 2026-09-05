package com.personal.happygallery.application.payment;

import com.personal.happygallery.adapter.out.persistence.coupon.IssuedCouponRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.application.cart.port.in.CartUseCase;
import com.personal.happygallery.application.cart.port.out.CartItemStorePort;
import com.personal.happygallery.domain.cart.CartItem;
import com.personal.happygallery.application.coupon.port.in.CouponAdminUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponDefinitionCommand;
import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.payment.PaymentConfirmClaimTransactionService.PgConfirmationRequired;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentAbandonUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentAttemptExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareResult;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.reward.RewardBenefitService;
import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.reward.RewardReservationStatus;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.personal.happygallery.support.TestFixtures.inventory;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@UseCaseIT
@TestPropertySource(properties = "app.order.shipping-fee=3000")
class OrderPaymentBenefitUseCaseIT {

    @Autowired CartUseCase cartUseCase;
    @Autowired CartItemStorePort cartItemStorePort;
    @Autowired PaymentPrepareUseCase prepareUseCase;
    @Autowired PaymentConfirmUseCase confirmUseCase;
    @Autowired PaymentAttemptExpiryBatchUseCase expiryUseCase;
    @Autowired PaymentAbandonUseCase abandonUseCase;
    @Autowired PaymentConfirmClaimTransactionService claimTransactionService;
    @Autowired PaymentReconciliationTransactionService reconciliationTransactionService;
    @Autowired PaymentAttemptReaderPort attemptReader;
    @Autowired CouponAdminUseCase couponAdminUseCase;
    @Autowired CouponMemberUseCase couponMemberUseCase;
    @Autowired IssuedCouponRepository issuedCouponRepository;
    @Autowired RewardBenefitService rewardBenefitService;
    @Autowired RewardQueryUseCase rewardQueryUseCase;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemPort orderItemPort;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoBean PaymentPort paymentProvider;

    @BeforeEach
    void setUp() {
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.success(
                        "confirmed-benefit-payment-key", "CARD", "2026-08-08T10:00:00+09:00"));
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearCartData();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("회원 주문은 배송비를 제외하고 쿠폰과 적립금을 품목에 비례 배분해 확정한다")
    void confirm_appliesAndAllocatesBenefitsExcludingShippingFee() {
        User user = createUser("benefit-confirm@example.com", "01081001001");
        Product first = createProduct("7천원 상품", 7_000L, 1);
        Product second = createProduct("3천원 상품", 3_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 4_000L);

        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(first.getId(), 1), new OrderItemRef(second.getId(), 1)),
                FulfillmentType.SHIPPING,
                coupon.getId(),
                4_000L);
        PaymentAttempt attempt = attempt(prepared);

        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(7_000L);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
            softly.assertThat(issuedCoupon(coupon).getPaymentAttemptId()).isEqualTo(attempt.getId());
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).availableBalance()).isZero();
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isEqualTo(4_000L);
            softly.assertThat(rewardReservationStatus(attempt.getId()))
                    .isEqualTo(RewardReservationStatus.RESERVED.name());
        });

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand("benefit-payment-key", prepared, user));

        Order order = orderRepository.findById(result.domainId()).orElseThrow();
        OrderItem firstItem = findItem(order, first.getId());
        OrderItem secondItem = findItem(order, second.getId());
        assertSoftly(softly -> {
            softly.assertThat(order.getProductAmount()).isEqualTo(10_000L);
            softly.assertThat(order.getShippingFee()).isEqualTo(3_000L);
            softly.assertThat(order.getCouponDiscountAmount()).isEqualTo(2_000L);
            softly.assertThat(order.getRewardUsedAmount()).isEqualTo(4_000L);
            softly.assertThat(order.getTotalAmount()).isEqualTo(11_000L);
            softly.assertThat(order.getPgPaidAmount()).isEqualTo(7_000L);
            softly.assertThat(order.getRewardEarnBase()).isEqualTo(4_000L);
            softly.assertThat(order.getIssuedCouponId()).isEqualTo(coupon.getId());
            softly.assertThat(firstItem.getCouponDiscountAmount()).isEqualTo(1_400L);
            softly.assertThat(firstItem.getRewardUsedAmount()).isEqualTo(2_800L);
            softly.assertThat(firstItem.getNetPaidAmount()).isEqualTo(2_800L);
            softly.assertThat(secondItem.getCouponDiscountAmount()).isEqualTo(600L);
            softly.assertThat(secondItem.getRewardUsedAmount()).isEqualTo(1_200L);
            softly.assertThat(secondItem.getNetPaidAmount()).isEqualTo(1_200L);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.REDEEMED);
            softly.assertThat(issuedCoupon(coupon).getUsedOrderId()).isEqualTo(order.getId());
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isZero();
            softly.assertThat(rewardReservationStatus(attempt.getId()))
                    .isEqualTo(RewardReservationStatus.USED.name());
        });
        verify(paymentProvider).confirm(
                "benefit-payment-key", prepared.orderId(), 7_000L, prepared.orderId());
    }

    @Test
    @DisplayName("쿠폰과 적립금이 상품 금액 전부를 충당하면 PG 호출 없이 0원 주문을 확정한다")
    void confirm_zeroAmount_skipsPgAndConsumesBenefits() {
        User user = createUser("benefit-zero@example.com", "01081001002");
        Product product = createProduct("0원 결제 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 8_000L);

        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP,
                coupon.getId(),
                8_000L);

        PaymentConfirmUseCase.ConfirmResult result = confirmUseCase.confirm(
                customerCommand(null, prepared, user));

        Order order = orderRepository.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isZero();
            softly.assertThat(order.getPgPaidAmount()).isZero();
            softly.assertThat(order.getCouponDiscountAmount()).isEqualTo(2_000L);
            softly.assertThat(order.getRewardUsedAmount()).isEqualTo(8_000L);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.REDEEMED);
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isZero();
            softly.assertThat(attempt(prepared).getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
        });
        verifyNoInteractions(paymentProvider);
    }

    @Test
    @DisplayName("최초 PG 호출의 명시적 최종 실패는 FAILED로 종결하고 혜택 예약을 해제한다")
    void confirm_initialFinalFailure_releasesReservations() {
        User user = createUser("benefit-initial-failure@example.com", "01081001007");
        Product product = createProduct("최초 실패 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 1_000L);
        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP,
                coupon.getId(),
                1_000L);
        PaymentAttempt attempt = attempt(prepared);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(PaymentConfirmResult.failure("PG 최종 거절"));

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("benefit-initial-failure-key", prepared, user)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_FAILED));

        assertBenefitsReleased(user, coupon, attempt);
        assertThat(attempt(prepared).getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
    }

    @Test
    @DisplayName("재시도 후 최종 실패는 대사로 격리하고 느은 승인을 혜택 예약으로 복구한다")
    void confirm_retryableThenFinalFailure_reconcilesAndKeepsReservationForLateApproval() {
        User user = createUser("benefit-failure@example.com", "01081001003");
        Product product = createProduct("실패 결제 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 1_000L);
        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP,
                coupon.getId(),
                1_000L);
        PaymentAttempt attempt = attempt(prepared);
        when(paymentProvider.confirm(any(), any(), anyLong(), any()))
                .thenReturn(
                        PaymentConfirmResult.retryableFailure("PG 일시 장애"),
                        PaymentConfirmResult.failure("PG 최종 거절"));

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("benefit-failure-key", prepared, user)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_CONFIRM_RETRYABLE));
        assertSoftly(softly -> {
            softly.assertThat(attempt(prepared).getStatus()).isEqualTo(PaymentAttemptStatus.RETRYABLE);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isEqualTo(1_000L);
            softly.assertThat(rewardReservationStatus(attempt.getId()))
                    .isEqualTo(RewardReservationStatus.RESERVED.name());
        });

        assertThatThrownBy(() -> confirmUseCase.confirm(
                customerCommand("benefit-failure-key", prepared, user)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.PAYMENT_RECONCILIATION_REQUIRED));
        assertSoftly(softly -> {
            softly.assertThat(attempt(prepared).getStatus())
                    .isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isEqualTo(1_000L);
            softly.assertThat(rewardReservationStatus(attempt.getId()))
                    .isEqualTo(RewardReservationStatus.RESERVED.name());
        });

        claimTransactionService.reconcileLatePgApproval(
                customerCommand("benefit-failure-key", prepared, user),
                "late-confirmed-benefit-key",
                "CARD");
        PaymentConfirmUseCase.ConfirmResult recovered = confirmUseCase.confirm(
                customerCommand("benefit-failure-key", prepared, user));

        assertSoftly(softly -> {
            softly.assertThat(orderRepository.findById(recovered.domainId())).isPresent();
            softly.assertThat(attempt(prepared).getStatus()).isEqualTo(PaymentAttemptStatus.CONFIRMED);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.REDEEMED);
            softly.assertThat(rewardReservationStatus(attempt.getId()))
                    .isEqualTo(RewardReservationStatus.USED.name());
        });
        verify(paymentProvider, times(2)).confirm(
                "benefit-failure-key", prepared.orderId(), 7_000L, prepared.orderId());
    }

    @Test
    @DisplayName("결제를 종료하면 혜택을 즉시 다시 사용할 수 있고 이전 결제 승인은 차단된다")
    void abandonPending_releasesBenefitsOnceAndPreventsConfirmation() {
        User user = createUser("benefit-abandon@example.com", "01081001007");
        Product product = createProduct("결제 중단 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 1_000L);
        PrepareResult prepared = prepare(user, List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP, coupon.getId(), 1_000L);
        PaymentAttempt attempt = attempt(prepared);

        assertThatThrownBy(() -> abandonUseCase.abandon(prepared.orderId(), AuthContext.member(-1L), null))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        abandonUseCase.abandon(prepared.orderId(), AuthContext.member(user.getId()), null);
        abandonUseCase.abandon(prepared.orderId(), AuthContext.member(user.getId()), null);

        assertBenefitsReleased(user, coupon, attempt);
        assertThat(attempt(prepared).getPayloadEnc()).isNull();
        assertThatThrownBy(() -> confirmUseCase.confirm(customerCommand("abandoned-key", prepared, user)))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAYMENT_ATTEMPT_EXPIRED));
        PrepareResult retried = prepare(user, List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP, coupon.getId(), 1_000L);
        assertThat(retried.amount()).isEqualTo(7_000L);
        verifyNoInteractions(paymentProvider);
    }

    @ParameterizedTest
    @EnumSource(value = PaymentAttemptStatus.class, names = {"PENDING", "CANCELED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("승인이 시작된 결제는 종료 요청으로 상태나 혜택 예약을 변경하지 않는다")
    void abandonStartedPayment_preservesStateAndReservations(PaymentAttemptStatus status) {
        User user = createUser("benefit-started@example.com", "01081001008");
        Product product = createProduct("진행 중 결제 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 1_000L);
        PrepareResult prepared = prepare(user, List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP, coupon.getId(), 1_000L);
        jdbcTemplate.update("UPDATE payment_attempt SET status = ? WHERE id = ?", status.name(), attempt(prepared).getId());

        assertThatThrownBy(() -> abandonUseCase.abandon(prepared.orderId(), AuthContext.member(user.getId()), null))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        assertSoftly(softly -> {
            softly.assertThat(attempt(prepared).getStatus()).isEqualTo(status);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isEqualTo(1_000L);
        });
        verifyNoInteractions(paymentProvider);
    }

    @Test
    @DisplayName("30분이 지난 PENDING 주문 결제는 배치가 취소하면서 혜택 예약을 해제한다")
    void expirePendingBenefitPayment_releasesReservations() {
        User user = createUser("benefit-expiry@example.com", "01081001004");
        Product product = createProduct("만료 결제 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 1_000L);
        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP,
                coupon.getId(),
                1_000L);
        PaymentAttempt attempt = attempt(prepared);
        LocalDateTime expirationBoundary = LocalDateTime.ofInstant(
                clock.instant().minus(DefaultPaymentAttemptExpiryBatchService.PREPARE_TTL),
                ZoneOffset.UTC);
        jdbcTemplate.update(
                "UPDATE payment_attempt SET created_at = ? WHERE id = ?",
                expirationBoundary.minusSeconds(1),
                attempt.getId());

        expiryUseCase.expirePendingAttempts();

        assertBenefitsReleased(user, coupon, attempt);
        PaymentAttempt canceled = attempt(prepared);
        assertSoftly(softly -> {
            softly.assertThat(canceled.getStatus()).isEqualTo(PaymentAttemptStatus.CANCELED);
            softly.assertThat(canceled.getPayloadEnc()).isNull();
        });
    }

    @Test
    @DisplayName("PG 대사 필요 상태에는 예약을 유지하고 미승인 확정 시 해제한다")
    void reconciliationRequiredThenNotApproved_releasesOnlyAfterResolution() {
        User user = createUser("benefit-reconciliation@example.com", "01081001005");
        Product product = createProduct("대사 결제 상품", 10_000L, 1);
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 1_000L);
        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP,
                coupon.getId(),
                1_000L);
        PaymentAttempt attempt = attempt(prepared);
        PgConfirmationRequired processing = (PgConfirmationRequired)
                claimTransactionService.resolveConfirmationStep(
                        customerCommand("benefit-reconciliation-key", prepared, user));

        assertThat(claimTransactionService.tryRecordPgReconciliationRequired(
                processing.attemptId(), processing.processingToken(), "PG 조회 필요")).isTrue();
        assertSoftly(softly -> {
            softly.assertThat(attempt(prepared).getStatus())
                    .isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isEqualTo(1_000L);
        });

        reconciliationTransactionService.recordNotApproved(attempt.getId(), "PG 미승인 확인");

        assertBenefitsReleased(user, coupon, attempt);
        PaymentAttempt failed = attempt(prepared);
        assertSoftly(softly -> {
            softly.assertThat(failed.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            softly.assertThat(failed.getPayloadEnc()).isNull();
        });
    }

    @Test
    @DisplayName("0원 결제 승인 뒤 주문 생성이 실패하면 혜택 예약을 해제하고 FAILED로 종결한다")
    void zeroAmountFulfillmentFailure_releasesReservations() {
        User user = createUser("benefit-zero-failure@example.com", "01081001006");
        Product product = productStorePort.save(readyStockProduct("0원 실패 상품", 10_000L));
        Inventory availableInventory = inventoryStorePort.save(inventory(product, 1));
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 8_000L);
        PrepareResult prepared = prepare(
                user,
                List.of(new OrderItemRef(product.getId(), 1)),
                FulfillmentType.PICKUP,
                coupon.getId(),
                8_000L);
        PaymentAttempt attempt = attempt(prepared);
        availableInventory.deduct(1);
        inventoryStorePort.save(availableInventory);

        assertThatThrownBy(() -> confirmUseCase.confirm(customerCommand(null, prepared, user)))
                .isInstanceOf(InventoryNotEnoughException.class);

        assertBenefitsReleased(user, coupon, attempt);
        assertSoftly(softly -> {
            softly.assertThat(attempt(prepared).getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
            softly.assertThat(orderRepository.count()).isZero();
        });
        verifyNoInteractions(paymentProvider);
    }

    @Test
    @DisplayName("선택 구매는 선택 상품에만 혜택과 배송비를 적용하고 미선택 상품과 추가 수량을 남긴다")
    void selectedCartCheckout_preservesUnselectedItemsAndAddedQuantity() {
        User user = createUser("selected-cart@example.com", "01081001090");
        Product selected = createProduct("이번에 구매할 상품", 10_000L, 10);
        Product unselected = createProduct("다음에 구매할 상품", 30_000L, 10);
        CartItem selectedItem = cartItemStorePort.save(new CartItem(
                user.getId(), selected.getId(), 2, LocalDateTime.now(clock)));
        cartItemStorePort.save(new CartItem(user.getId(), unselected.getId(), 1, LocalDateTime.now(clock)));
        IssuedCoupon coupon = issueFixedCoupon(user, 2_000L);
        creditReward(user, 4_000L);
        String version = cartUseCase.getCart(user.getId()).cartVersion();
        PrepareResult prepared = prepareUseCase.prepare(new PrepareCommand(PaymentContext.ORDER,
                new OrderPayload(user.getId(), null, null, null, List.of(), true,
                        FulfillmentType.SHIPPING,
                        new ShippingAddress("회원", "01081001090", "06236", "서울시 강남구 1", null),
                        null, false, null, version, coupon.getId(), 4_000L, List.of(selectedItem.getId())),
                AuthContext.member(user.getId())));
        cartUseCase.updateItemQty(user.getId(), selectedItem.getId(), 3);
        var result = confirmUseCase.confirm(customerCommand("selected-cart-key", prepared, user));
        Order order = orderRepository.findById(result.domainId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(prepared.amount()).isEqualTo(17_000L);
            softly.assertThat(order.getProductAmount()).isEqualTo(20_000L);
            softly.assertThat(order.getShippingFee()).isEqualTo(3_000L);
            softly.assertThat(order.getCouponDiscountAmount()).isEqualTo(2_000L);
            softly.assertThat(order.getRewardUsedAmount()).isEqualTo(4_000L);
            softly.assertThat(orderItemPort.findByOrderIdIn(List.of(order.getId()))).hasSize(1);
            softly.assertThat(cartUseCase.getCart(user.getId()).items())
                    .extracting(CartUseCase.CartItemView::productId, CartUseCase.CartItemView::qty)
                    .containsExactlyInAnyOrder(
                            tuple(selected.getId(), 1),
                            tuple(unselected.getId(), 1));
        });
    }

    private PrepareResult prepare(User user,
                                  List<OrderItemRef> items,
                                  FulfillmentType fulfillmentType,
                                  Long issuedCouponId,
                                  long rewardAmount) {
        ShippingAddress shippingAddress = fulfillmentType == FulfillmentType.SHIPPING
                ? new ShippingAddress(
                        "혜택 회원", "01081000000", "06236",
                        "서울시 강남구 테헤란로 1", null)
                : null;
        return prepareUseCase.prepare(new PrepareCommand(
                PaymentContext.ORDER,
                new OrderPayload(
                        user.getId(), null, null, null, items, false,
                        fulfillmentType, shippingAddress, null, false, null, null,
                        issuedCouponId, rewardAmount),
                AuthContext.member(user.getId())));
    }

    private ConfirmCommand customerCommand(String paymentKey, PrepareResult prepared, User user) {
        return ConfirmCommand.customerRequest(
                paymentKey,
                prepared.orderId(),
                prepared.amount(),
                AuthContext.member(user.getId()),
                null);
    }

    private User createUser(String email, String phone) {
        return userStorePort.save(new User(email, "password-hash", "혜택 회원", phone));
    }

    private Product createProduct(String name, long price, int quantity) {
        Product product = productStorePort.save(readyStockProduct(name, price));
        inventoryStorePort.save(inventory(product, quantity));
        return product;
    }

    private IssuedCoupon issueFixedCoupon(User user, long discountAmount) {
        LocalDateTime now = LocalDateTime.now(clock);
        var definition = couponAdminUseCase.create(new CouponDefinitionCommand(
                "결제 통합 테스트 쿠폰",
                CouponDiscountType.FIXED,
                discountAmount,
                0L,
                null,
                now.minusDays(1),
                now.plusDays(30),
                true,
                true));
        return couponMemberUseCase.claim(user.getId(), definition.getId()).issuedCoupon();
    }

    private void creditReward(User user, long amount) {
        rewardBenefitService.accrue(user.getId(), null, amount, LocalDateTime.now(clock));
    }

    private PaymentAttempt attempt(PrepareResult prepared) {
        return attemptReader.findByOrderIdExternal(prepared.orderId()).orElseThrow();
    }

    private IssuedCoupon issuedCoupon(IssuedCoupon coupon) {
        return issuedCouponRepository.findById(coupon.getId()).orElseThrow();
    }

    private String rewardReservationStatus(Long attemptId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_reservations WHERE payment_attempt_id = ?",
                String.class,
                attemptId);
    }

    private OrderItem findItem(Order order, Long productId) {
        return orderItemPort.findByOrder(order).stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow();
    }

    private void assertBenefitsReleased(User user,
                                        IssuedCoupon coupon,
                                        PaymentAttempt attempt) {
        assertSoftly(softly -> {
            softly.assertThat(issuedCoupon(coupon).getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
            softly.assertThat(issuedCoupon(coupon).getPaymentAttemptId()).isNull();
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).availableBalance())
                    .isEqualTo(attempt.getAmount() == 0L ? 8_000L : 1_000L);
            softly.assertThat(rewardQueryUseCase.getWallet(user.getId()).reservedBalance()).isZero();
            softly.assertThat(rewardReservationStatus(attempt.getId()))
                    .isEqualTo(RewardReservationStatus.RELEASED.name());
        });
    }
}
