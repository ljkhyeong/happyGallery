package com.personal.happygallery.application.order;

import com.personal.happygallery.adapter.out.external.payment.FakePaymentProvider;
import com.personal.happygallery.adapter.out.persistence.coupon.CouponDefinitionRepository;
import com.personal.happygallery.adapter.out.persistence.coupon.IssuedCouponRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.dashboard.dto.TopProductSort;
import com.personal.happygallery.application.dashboard.port.out.SalesAnalyticsPort;
import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.out.OrderClaimItemPort;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import com.personal.happygallery.domain.order.OrderClaimType;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;

@UseCaseIT
class OrderClaimUseCaseIT {

    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired InventoryReaderPort inventoryReaderPort;
    @Autowired OrderStorePort orderStorePort;
    @Autowired OrderItemPort orderItemPort;
    @Autowired UserStorePort userStorePort;
    @Autowired OrderService orderService;
    @Autowired OrderApprovalUseCase orderApprovalUseCase;
    @Autowired OrderShippingUseCase orderShippingUseCase;
    @Autowired OrderClaimUseCase orderClaimUseCase;
    @Autowired AdminOrderClaimUseCase adminOrderClaimUseCase;
    @Autowired OrderClaimPort orderClaimPort;
    @Autowired OrderClaimItemPort orderClaimItemPort;
    @Autowired SalesAnalyticsPort salesAnalyticsPort;
    @Autowired CouponDefinitionRepository couponDefinitionRepository;
    @Autowired IssuedCouponRepository issuedCouponRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    @Autowired UserReaderPort userReaderPort;
    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean OrderRepository orderRepository;
    @MockitoSpyBean(name = "paymentProviderDelegate") FakePaymentProvider paymentProvider;

    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        orderHelper = new OrderTestHelper(
                productStorePort,
                inventoryStorePort,
                inventoryReaderPort,
                orderStorePort,
                orderItemPort,
                userStorePort,
                orderService);
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("배송 완료 클레임을 환불 승인하면 재고와 클레임 상태가 함께 종결된다")
    @Test
    void approveRefundClaim_restoresInventoryAndCompletesAfterRefund() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createReadyStockPaidShippingOrder("파손 확인 상품", 40_000L, 3_000L);
        Order order = fixture.order();
        order.recordPaymentKey("claim-payment-key");
        orderStorePort.save(order);
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderShippingUseCase.prepareShipping(order.getId(), 1L);
        orderShippingUseCase.markShipped(order.getId(), "테스트택배", "TRACK-1", 1L);
        orderShippingUseCase.markDelivered(order.getId(), 1L);
        Long orderItemId = orderItemPort.findByOrder(order).getFirst().getId();

        var claim = orderClaimUseCase.requestMemberClaim(
                order.getId(),
                order.getUserId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.DAMAGED,
                        OrderClaimResolution.REFUND,
                        "수령한 상품이 파손되었습니다.",
                        List.of(new OrderClaimUseCase.Item(orderItemId, 1))));

        adminOrderClaimUseCase.resolve(
                claim.id(),
                1L,
                new AdminOrderClaimUseCase.ResolveCommand(
                        true, 43_000L, true, "반품 확인 후 전액 환불"));

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            var completed = orderClaimUseCase
                    .listMemberClaims(order.getId(), order.getUserId()).getFirst();
            assertSoftly(softly -> {
                softly.assertThat(completed.status()).isEqualTo(OrderClaimStatus.COMPLETED);
                softly.assertThat(completed.refundAmount()).isEqualTo(43_000L);
                softly.assertThat(completed.refundStatus()).isEqualTo(RefundStatus.SUCCEEDED);
                softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId())
                        .getQuantity()).isEqualTo(1);
            });
        });

        assertThatThrownBy(() -> orderClaimUseCase.requestMemberClaim(
                order.getId(),
                order.getUserId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.OTHER,
                        OrderClaimResolution.EXCHANGE,
                        "동일 상품으로 다시 요청합니다.",
                        List.of(new OrderClaimUseCase.Item(orderItemId, 1)))))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("주문 수량을 초과");
    }

    @DisplayName("여러 상품의 부분 환불액은 승인 합계를 보존하며 상품별 매출에서 정확히 차감된다")
    @Test
    void approvePartialRefundClaim_allocatesExactProductRevenue() {
        Product firstProduct = orderHelper.createReadyStockProduct("부분 환불 상품 1", 10_000L, 1);
        Product secondProduct = orderHelper.createReadyStockProduct("부분 환불 상품 2", 20_000L, 1);
        var member = orderHelper.createMemberOwner();
        Order order = orderService.createMemberOrder(
                member.getId(),
                List.of(
                        new OrderService.OrderItemRequest(
                                firstProduct.getId(), firstProduct.getName(), 1, firstProduct.getPrice()),
                        new OrderService.OrderItemRequest(
                                secondProduct.getId(), secondProduct.getName(), 1, secondProduct.getPrice())),
                FulfillmentType.SHIPPING,
                new ShippingAddress(
                        "주문 테스트 회원", "01012345678", "06236", "서울시 강남구 테헤란로 1", null),
                3_000L);
        order.recordPaymentKey("partial-claim-payment-key");
        orderStorePort.save(order);
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderShippingUseCase.prepareShipping(order.getId(), 1L);
        orderShippingUseCase.markShipped(order.getId(), "테스트택배", "TRACK-4", 1L);
        orderShippingUseCase.markDelivered(order.getId(), 1L);
        var orderItemsByProductId = orderItemPort.findByOrder(order).stream()
                .collect(Collectors.toMap(item -> item.getProductId(), Function.identity()));

        var claim = orderClaimUseCase.requestMemberClaim(
                order.getId(),
                order.getUserId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.DAMAGED,
                        OrderClaimResolution.REFUND,
                        "두 상품의 일부 금액을 환불합니다.",
                        List.of(
                                new OrderClaimUseCase.Item(
                                        orderItemsByProductId.get(firstProduct.getId()).getId(), 1),
                                new OrderClaimUseCase.Item(
                                        orderItemsByProductId.get(secondProduct.getId()).getId(), 1))));

        adminOrderClaimUseCase.resolve(
                claim.id(),
                1L,
                new AdminOrderClaimUseCase.ResolveCommand(
                        true, 10_001L, false, "부분 환불 승인"));

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(
                orderClaimUseCase.listMemberClaims(order.getId(), order.getUserId()).getFirst().status())
                .isEqualTo(OrderClaimStatus.COMPLETED));

        var allocationsByOrderItemId = orderClaimItemPort.findByClaimIdIn(List.of(claim.id())).stream()
                .collect(Collectors.toMap(
                        item -> item.getOrderItemId(),
                        item -> item.getApprovedRefundAmount()));
        var topProductsById = salesAnalyticsPort.findTopProducts(
                        LocalDate.of(2026, 3, 1),
                        LocalDate.of(2026, 3, 1),
                        10,
                        TopProductSort.REVENUE).stream()
                .collect(Collectors.toMap(product -> product.productId(), Function.identity()));

        assertSoftly(softly -> {
            softly.assertThat(allocationsByOrderItemId)
                    .containsEntry(
                            orderItemsByProductId.get(firstProduct.getId()).getId(), 3_334L)
                    .containsEntry(
                            orderItemsByProductId.get(secondProduct.getId()).getId(), 6_667L);
            softly.assertThat(topProductsById.get(firstProduct.getId()).totalRevenue())
                    .isEqualTo(6_666L);
            softly.assertThat(topProductsById.get(secondProduct.getId()).totalRevenue())
                    .isEqualTo(13_333L);
        });
    }

    @DisplayName("쿠폰·적립금 혼합 주문의 전액 환불은 상품 순매출에서 쿠폰 후 고객 반환액을 차감한다")
    @Test
    void topProducts_mixedFullRefund_usesCouponAdjustedCustomerAmount() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createReadyStockPaidShippingOrder("혼합 환불 상품", 40_000L, 3_000L);
        Order order = fixture.order();
        Long orderItemId = orderItemPort.findByOrder(order).getFirst().getId();
        LocalDateTime usedAt = LocalDate.of(2026, 3, 1).atTime(10, 0);
        CouponDefinition couponDefinition = couponDefinitionRepository.saveAndFlush(
                new CouponDefinition(
                        "혼합 환불 분석 쿠폰",
                        CouponDiscountType.FIXED,
                        10_000L,
                        0L,
                        null,
                        usedAt.minusDays(1),
                        usedAt.plusDays(30),
                        true,
                        false));
        IssuedCoupon issuedCoupon = issuedCouponRepository.saveAndFlush(
                new IssuedCoupon(couponDefinition.getId(), order.getUserId(), usedAt.minusHours(1)));
        PaymentAttempt paymentAttempt = paymentAttemptRepository.saveAndFlush(
                PaymentAttempt.startForMember(
                        "mixed-refund-analytics-attempt",
                        PaymentContext.ORDER,
                        28_000L,
                        "{}",
                        order.getUserId()));
        issuedCoupon.reserve(paymentAttempt.getId(), usedAt.minusMinutes(30));
        issuedCoupon.redeem(paymentAttempt.getId(), order.getId(), usedAt);
        issuedCouponRepository.saveAndFlush(issuedCoupon);
        jdbcTemplate.update("""
                UPDATE orders
                SET total_amount = 33000,
                    product_amount = 40000,
                    coupon_discount_amount = 10000,
                    reward_used_amount = 5000,
                    pg_paid_amount = 28000,
                    reward_earn_base = 25000,
                    issued_coupon_id = ?
                WHERE id = ?
                """, issuedCoupon.getId(), order.getId());
        jdbcTemplate.update("""
                UPDATE order_items
                SET gross_amount = 40000,
                    coupon_discount_amount = 10000,
                    reward_used_amount = 5000,
                    net_paid_amount = 25000
                WHERE id = ?
                """, orderItemId);
        jdbcTemplate.update("""
                INSERT INTO refunds
                    (order_id, amount, customer_refund_amount,
                     reward_restore_amount, reward_revoke_amount, restore_coupon,
                     status, payment_key, refund_transaction_key, succeeded_at, idempotency_key)
                VALUES (?, 28000, 33000, 5000, 0, TRUE,
                        'SUCCEEDED', 'mixed-payment-key', 'mixed-refund-key', ?, 'mixed-refund-idempotency')
                """, order.getId(), LocalDate.of(2026, 3, 1).atTime(12, 0));

        var topProduct = salesAnalyticsPort.findTopProducts(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 1),
                10,
                TopProductSort.REVENUE).stream()
                .filter(product -> product.productId().equals(fixture.product().getId()))
                .findFirst()
                .orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(topProduct.totalRevenue()).isZero();
            softly.assertThat(topProduct.totalQuantity()).isZero();
        });
    }

    @DisplayName("DB는 다른 주문의 상품과 클레임 환불을 교차 연결하지 못하게 막는다")
    @Test
    void orderClaimDatabaseConstraints_rejectCrossOrderReferences() {
        Order firstOrder = orderHelper
                .createReadyStockPaidShippingOrder("클레임 소유 주문", 30_000L, 3_000L)
                .order();
        Order secondOrder = orderHelper
                .createReadyStockPaidShippingOrder("다른 주문", 40_000L, 3_000L)
                .order();
        orderApprovalUseCase.approve(firstOrder.getId(), 1L);
        orderShippingUseCase.prepareShipping(firstOrder.getId(), 1L);
        orderShippingUseCase.markShipped(firstOrder.getId(), "테스트택배", "TRACK-5", 1L);
        orderShippingUseCase.markDelivered(firstOrder.getId(), 1L);
        Long firstOrderItemId = orderItemPort.findByOrder(firstOrder).getFirst().getId();
        Long secondOrderItemId = orderItemPort.findByOrder(secondOrder).getFirst().getId();
        var claim = orderClaimUseCase.requestMemberClaim(
                firstOrder.getId(),
                firstOrder.getUserId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.DAMAGED,
                        OrderClaimResolution.REFUND,
                        "DB 교차 연결을 검증합니다.",
                        List.of(new OrderClaimUseCase.Item(firstOrderItemId, 1))));

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> jdbcTemplate.update("""
                            INSERT INTO order_claim_items
                                (claim_id, order_id, order_item_id, quantity)
                            VALUES (?, ?, ?, ?)
                            """, claim.id(), secondOrder.getId(), secondOrderItemId, 1))
                    .isInstanceOf(DataIntegrityViolationException.class);
            softly.assertThatThrownBy(() -> jdbcTemplate.update("""
                            INSERT INTO refunds
                                (order_id, order_claim_id, amount, customer_refund_amount,
                                 reward_restore_amount, reward_revoke_amount, restore_coupon,
                                 status, idempotency_key)
                            VALUES (?, ?, ?, ?, 0, 0, FALSE, 'REQUESTED', ?)
                            """, secondOrder.getId(), claim.id(), 1_000L, 1_000L,
                            "cross-order-claim-refund"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        });
    }

    @DisplayName("교환 승인 시 반품 재고를 복구해도 교환품 출고 수량을 다시 차감한다")
    @Test
    void approveExchangeClaim_deductsReplacementInventory() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createReadyStockPaidShippingOrder("교환 확인 상품", 40_000L, 3_000L);
        Order order = fixture.order();
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderShippingUseCase.prepareShipping(order.getId(), 1L);
        orderShippingUseCase.markShipped(order.getId(), "테스트택배", "TRACK-2", 1L);
        orderShippingUseCase.markDelivered(order.getId(), 1L);
        Long orderItemId = orderItemPort.findByOrder(order).getFirst().getId();

        var claim = orderClaimUseCase.requestMemberClaim(
                order.getId(),
                order.getUserId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.WRONG_ITEM,
                        OrderClaimResolution.EXCHANGE,
                        "다른 상품이 배송되었습니다.",
                        List.of(new OrderClaimUseCase.Item(orderItemId, 1))));

        var approved = adminOrderClaimUseCase.resolve(
                claim.id(),
                1L,
                new AdminOrderClaimUseCase.ResolveCommand(
                        true, null, true, "반품 확인 후 교환"));

        assertSoftly(softly -> {
            softly.assertThat(approved.status()).isEqualTo(OrderClaimStatus.EXCHANGE_APPROVED);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId())
                    .getQuantity()).isZero();
        });
    }

    @DisplayName("회원 탈퇴와 클레임 접수가 동시에 시작되어도 둘 다 성공하지 않는다")
    @Test
    void requestMemberClaim_concurrentWithdrawal_serializesOnUser() throws Exception {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createReadyStockPaidShippingOrder("탈퇴 경합 상품", 40_000L, 3_000L);
        Order order = fixture.order();
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderShippingUseCase.prepareShipping(order.getId(), 1L);
        orderShippingUseCase.markShipped(order.getId(), "테스트택배", "TRACK-3", 1L);
        orderShippingUseCase.markDelivered(order.getId(), 1L);
        Long orderItemId = orderItemPort.findByOrder(order).getFirst().getId();
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> claim = executor.submit(() -> {
                start.await();
                try {
                    orderClaimUseCase.requestMemberClaim(
                            order.getId(),
                            order.getUserId(),
                            new OrderClaimUseCase.RequestCommand(
                                    OrderClaimType.DAMAGED,
                                    OrderClaimResolution.REFUND,
                                    "탈퇴와 동시에 접수합니다.",
                                    List.of(new OrderClaimUseCase.Item(orderItemId, 1))));
                    return true;
                } catch (HappyGalleryException ignored) {
                    return false;
                }
            });
            Future<Boolean> withdrawal = executor.submit(() -> {
                start.await();
                try {
                    User user = userReaderPort.findById(order.getUserId()).orElseThrow();
                    accountLifecycleUseCase.withdraw(new WithdrawCommand(
                            user.getId(), user.getCredentialVersion(), true));
                    return true;
                } catch (HappyGalleryException ignored) {
                    return false;
                }
            });

            start.countDown();
            boolean claimSucceeded = claim.get(5, TimeUnit.SECONDS);
            boolean withdrawalSucceeded = withdrawal.get(5, TimeUnit.SECONDS);

            assertSoftly(softly -> {
                softly.assertThat(claimSucceeded && withdrawalSucceeded).isFalse();
                softly.assertThat(claimSucceeded || withdrawalSucceeded).isTrue();
                softly.assertThat(orderClaimPort.findByOrderIdOrderByRequestedAtDesc(order.getId()))
                        .hasSize(claimSucceeded ? 1 : 0);
            });
        }
    }

    @DisplayName("환불 완료와 신규 클레임이 같은 주문에서 경합해도 교착 없이 수량과 상태를 보존한다")
    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void refundCompletion_concurrentMemberClaim_preservesClaimInvariants() throws Exception {
        Product product = orderHelper.createReadyStockProduct("환불 완료 경합 상품", 20_000L, 2);
        User member = orderHelper.createMemberOwner();
        Order order = orderService.createMemberOrder(
                member.getId(),
                List.of(new OrderService.OrderItemRequest(
                        product.getId(), product.getName(), 2, product.getPrice())),
                FulfillmentType.SHIPPING,
                new ShippingAddress(
                        "주문 테스트 회원", "01012345678", "06236", "서울시 강남구 테헤란로 1", null),
                3_000L);
        order.recordPaymentKey("claim-race-payment-key");
        orderStorePort.save(order);
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderShippingUseCase.prepareShipping(order.getId(), 1L);
        orderShippingUseCase.markShipped(order.getId(), "테스트택배", "CLAIM-RACE-TRACK", 1L);
        orderShippingUseCase.markDelivered(order.getId(), 1L);
        Long orderItemId = orderItemPort.findByOrder(order).getFirst().getId();

        var refundClaim = orderClaimUseCase.requestMemberClaim(
                order.getId(),
                member.getId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.DAMAGED,
                        OrderClaimResolution.REFUND,
                        "첫 번째 수량을 환불합니다.",
                        List.of(new OrderClaimUseCase.Item(orderItemId, 1))));

        CountDownLatch paymentCallEntered = new CountDownLatch(1);
        CountDownLatch allowPaymentResult = new CountDownLatch(1);
        CountDownLatch refundOrderLockAttempted = new CountDownLatch(1);
        CountDownLatch memberOrderLockAttempted = new CountDownLatch(1);
        AtomicReference<Thread> memberClaimThread = new AtomicReference<>();
        Answer<?> orderRepositoryDelegate = mockingDetails(orderRepository)
                .getMockCreationSettings()
                .getDefaultAnswer();
        doAnswer(invocation -> {
            paymentCallEntered.countDown();
            awaitSignal(allowPaymentResult, "PG 환불 완료 허용 신호를 기다리지 못했습니다.");
            return RefundResult.success("claim-race-refund-transaction-key");
        }).when(paymentProvider).refund(any(), anyLong(), any());
        doAnswer(invocation -> {
            Thread currentThread = Thread.currentThread();
            if (currentThread.getName().startsWith("refund-")) {
                refundOrderLockAttempted.countDown();
            } else if (currentThread == memberClaimThread.get()) {
                memberOrderLockAttempted.countDown();
            }
            return orderRepositoryDelegate.answer(invocation);
        }).when(orderRepository).findByIdForUpdate(order.getId());

        adminOrderClaimUseCase.resolve(
                refundClaim.id(),
                1L,
                new AdminOrderClaimUseCase.ResolveCommand(
                        true, 20_000L, true, "첫 번째 수량 환불 승인"));
        awaitSignal(paymentCallEntered, "비동기 PG 환불 호출이 시작되지 않았습니다.");

        AtomicReference<Future<OrderClaimView>> memberClaimFuture = new AtomicReference<>();
        try (var executor = Executors.newSingleThreadExecutor()) {
            try {
                new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                    orderRepository.findByIdForUpdate(order.getId()).orElseThrow();

                    allowPaymentResult.countDown();
                    awaitSignal(
                            refundOrderLockAttempted,
                            "환불 완료 트랜잭션이 주문 잠금을 시도하지 않았습니다.");

                    memberClaimFuture.set(executor.submit(() -> {
                        memberClaimThread.set(Thread.currentThread());
                        return orderClaimUseCase.requestMemberClaim(
                                order.getId(),
                                member.getId(),
                                new OrderClaimUseCase.RequestCommand(
                                        OrderClaimType.OTHER,
                                        OrderClaimResolution.EXCHANGE,
                                        "남은 한 수량을 교환합니다.",
                                        List.of(new OrderClaimUseCase.Item(orderItemId, 1))));
                    }));
                    awaitSignal(
                            memberOrderLockAttempted,
                            "신규 클레임 트랜잭션이 주문 잠금을 시도하지 않았습니다.");
                });
            } finally {
                allowPaymentResult.countDown();
            }

            OrderClaimView memberClaim = memberClaimFuture.get().get(5, TimeUnit.SECONDS);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
                List<OrderClaimView> claims = orderClaimUseCase.listMemberClaims(
                        order.getId(), member.getId());
                Map<Long, OrderClaimStatus> statusesByClaimId = claims.stream()
                        .collect(Collectors.toMap(OrderClaimView::id, OrderClaimView::status));
                List<Long> claimIds = claims.stream().map(OrderClaimView::id).toList();
                var claimItems = orderClaimItemPort.findByClaimIdIn(claimIds);

                assertSoftly(softly -> {
                    softly.assertThat(statusesByClaimId)
                            .hasSize(2)
                            .containsEntry(refundClaim.id(), OrderClaimStatus.COMPLETED)
                            .containsEntry(memberClaim.id(), OrderClaimStatus.REQUESTED);
                    softly.assertThat(claimItems)
                            .hasSize(2)
                            .allMatch(item -> item.getOrderItemId().equals(orderItemId));
                    softly.assertThat(claimItems.stream()
                                    .mapToInt(item -> item.getQuantity())
                                    .sum())
                            .isEqualTo(2);
                    softly.assertThat(orderStateProbe.getInventoryByProductId(product.getId())
                                    .getQuantity())
                            .isEqualTo(1);
                });
            });
        }
    }

    @DisplayName("관리자는 같은 상태의 오래된 주문 클레임까지 커서로 이어서 조회한다")
    @Test
    void listAdminClaims_usesStableCursorWithinStatus() {
        var older = createRequestedClaim("커서 이전 클레임");
        var newer = createRequestedClaim("커서 최신 클레임");

        var firstPage = adminOrderClaimUseCase.list(OrderClaimStatus.REQUESTED, null, 1);
        var secondPage = adminOrderClaimUseCase.list(
                OrderClaimStatus.REQUESTED, firstPage.nextCursor(), 1);

        assertSoftly(softly -> {
            softly.assertThat(firstPage.content())
                    .extracting(OrderClaimView::id)
                    .containsExactly(newer.id());
            softly.assertThat(firstPage.hasMore()).isTrue();
            softly.assertThat(firstPage.nextCursor()).isNotBlank();
            softly.assertThat(secondPage.content())
                    .extracting(OrderClaimView::id)
                    .containsExactly(older.id());
            softly.assertThat(secondPage.hasMore()).isFalse();
            softly.assertThat(secondPage.nextCursor()).isNull();
        });
    }

    private OrderClaimView createRequestedClaim(String productName) {
        Order order = orderHelper
                .createReadyStockPaidShippingOrder(productName, 40_000L, 3_000L)
                .order();
        orderApprovalUseCase.approve(order.getId(), 1L);
        orderShippingUseCase.prepareShipping(order.getId(), 1L);
        orderShippingUseCase.markShipped(order.getId(), "테스트택배", "CURSOR-TRACK", 1L);
        orderShippingUseCase.markDelivered(order.getId(), 1L);
        Long orderItemId = orderItemPort.findByOrder(order).getFirst().getId();
        return orderClaimUseCase.requestMemberClaim(
                order.getId(),
                order.getUserId(),
                new OrderClaimUseCase.RequestCommand(
                        OrderClaimType.DAMAGED,
                        OrderClaimResolution.REFUND,
                        "관리자 커서 조회를 확인합니다.",
                        List.of(new OrderClaimUseCase.Item(orderItemId, 1))));
    }

    private static void awaitSignal(CountDownLatch latch, String failureMessage) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as(failureMessage)
                    .isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시 경합 신호 대기가 중단되었습니다.", exception);
        }
    }
}
