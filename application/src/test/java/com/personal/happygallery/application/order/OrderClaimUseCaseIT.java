package com.personal.happygallery.application.order;

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
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import com.personal.happygallery.domain.order.OrderClaimType;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;

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
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycleUseCase;
    @Autowired UserReaderPort userReaderPort;
    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;

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
                                (order_id, order_claim_id, amount, status, idempotency_key)
                            VALUES (?, ?, ?, 'REQUESTED', ?)
                            """, secondOrder.getId(), claim.id(), 1_000L, "cross-order-claim-refund"))
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
}
