package com.personal.happygallery.application.order;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] §8.4 픽업 만료 배치 검증.
 *
 * <p>Proof (§8.4 DoD): 기성품 픽업 만료는 환불·재고 복구하고,
 * 주문제작 미수령은 환불 없이 종료한다.
 */
@UseCaseIT
class PickupExpireBatchUseCaseIT {

    private static final long ADMIN_ID = 1L;

    @Autowired PickupExpireBatchUseCase pickupExpireBatchService;
    @Autowired PickupExpireProcessor pickupExpireProcessor;
    @Autowired OrderPickupUseCase orderPickupService;
    @Autowired OrderApprovalUseCase orderApprovalService;
    @Autowired OrderProductionUseCase orderProductionService;
    @Autowired OrderService orderService;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired InventoryReaderPort inventoryReaderPort;
    @Autowired OrderStorePort orderStorePort;
    @Autowired OrderItemPort orderItemPort;
    @Autowired FulfillmentPort fulfillmentPort;
    @Autowired UserStorePort userStorePort;
    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        orderHelper = new OrderTestHelper(
                productStorePort, inventoryStorePort, inventoryReaderPort, orderStorePort, orderItemPort,
                userStorePort, orderService, clock);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.4): 픽업 마감 초과 → 상품 유형별 환불 정책 적용
    // -----------------------------------------------------------------------

    @DisplayName("픽업 기한이 지난 기성품 주문은 환불되고 재고가 복구된다")
    @Test
    void expirePickups_readyStockExpired_refundsAndRestoresInventory() {
        OrderTestHelper.OrderFixture fixture = orderHelper.createReadyStockPaidOrder("픽업 테스트 상품", 50000L);
        Order order = fixture.order();
        assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity()).isEqualTo(0);

        // 승인 → APPROVED_FULFILLMENT_PENDING
        orderApprovalService.approve(order.getId(), ADMIN_ID);

        // 픽업 준비 완료 (마감 시각: 과거)
        markPickupReadyWithExpiredDeadline(order.getId());

        Order afterReady = orderStateProbe.getOrder(order.getId());

        // 배치 실행
        BatchResult result = pickupExpireBatchService.expirePickups();

        // 상태 확인
        Order expired = orderStateProbe.getOrder(order.getId());

        // 재고 복구 확인
        int restoredQuantity = orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity();

        // 환불 기록 확인
        var refunds = orderStateProbe.refunds();
        var decisions = orderStateProbe.orderApprovalHistoryOrdered(order.getId()).stream()
                .map(history -> history.getDecision())
                .toList();

        assertSoftly(softly -> {
            softly.assertThat(afterReady.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(expired.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED);
            softly.assertThat(restoredQuantity).isEqualTo(1);
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refunds.getFirst().getOrderId()).isEqualTo(order.getId());
            softly.assertThat(decisions).containsExactly(
                    OrderApprovalDecision.APPROVE,
                    OrderApprovalDecision.PICKUP_READY,
                    OrderApprovalDecision.PICKUP_EXPIRED);
        });
    }

    @DisplayName("픽업 기한이 지난 주문제작 상품은 환불과 재고 복구 없이 만료된다")
    @Test
    void expirePickups_madeToOrderExpired_doesNotRefundOrRestoreInventory() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createMadeToOrderPaidOrder("미수령 주문제작 상품", 200000L);
        Order order = fixture.order();

        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.completeProduction(order.getId(), 1L);
        markPickupReadyWithExpiredDeadline(order.getId());

        BatchResult result = pickupExpireBatchService.expirePickups();

        Order expired = orderStateProbe.getOrder(order.getId());
        int remainingQuantity = orderStateProbe
                .getInventoryByProductId(fixture.product().getId())
                .getQuantity();
        var decisions = orderStateProbe.orderApprovalHistoryOrdered(order.getId()).stream()
                .map(history -> history.getDecision())
                .toList();

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(expired.getStatus()).isEqualTo(OrderStatus.PICKUP_FORFEITED);
            softly.assertThat(remainingQuantity).isZero();
            softly.assertThat(orderStateProbe.refundCount()).isZero();
            softly.assertThat(decisions).containsExactly(
                    OrderApprovalDecision.APPROVE,
                    OrderApprovalDecision.PRODUCTION_COMPLETE,
                    OrderApprovalDecision.PICKUP_READY,
                    OrderApprovalDecision.PICKUP_FORFEITED);
        });
    }

    // -----------------------------------------------------------------------
    // 마감 미경과 → 배치가 처리하지 않음
    // -----------------------------------------------------------------------

    @DisplayName("픽업 기한이 남은 주문은 만료 처리되지 않는다")
    @Test
    void expirePickups_futureDeadline_notExpired() {
        Order order = orderHelper.createReadyStockPaidOrder("미만료 픽업 상품", 30000L).order();

        orderApprovalService.approve(order.getId(), ADMIN_ID);

        // 픽업 준비 완료 (마감 시각: 미래)
        LocalDateTime futureDeadline = LocalDateTime.now(clock).plusDays(1);
        orderPickupService.markPickupReady(order.getId(), futureDeadline, 1L);

        // 배치 실행 → 0건 처리
        BatchResult result = pickupExpireBatchService.expirePickups();

        // 상태 유지 확인
        Order unchanged = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(0);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
        });
    }

    @DisplayName("픽업 만료 배치에서 한 건이 실패해도 다음 주문을 계속 처리하고 실패를 집계한다")
    @Test
    void expirePickups_whenOneOrderFails_continuesNextOrderAndCountsFailure() {
        OrderTestHelper.OrderFixture failedFixture = orderHelper.createReadyStockPaidOrder("픽업 만료 실패 상품", 41000L);
        OrderTestHelper.OrderFixture successFixture = orderHelper.createReadyStockPaidOrder("픽업 만료 성공 상품", 42000L);
        Order failedOrder = failedFixture.order();
        Order successOrder = successFixture.order();

        orderApprovalService.approve(failedOrder.getId(), ADMIN_ID);
        orderApprovalService.approve(successOrder.getId(), ADMIN_ID);

        markPickupReadyWithExpiredDeadline(failedOrder.getId());
        markPickupReadyWithExpiredDeadline(successOrder.getId());

        // 실패 케이스 유도: 재고 레코드가 사라진 상태에서 복구 시도하면 NotFoundException 발생
        inventoryStorePort.deleteById(failedFixture.product().getId());

        BatchResult result = pickupExpireBatchService.expirePickups();

        Order failedUpdated = orderStateProbe.getOrder(failedOrder.getId());
        Order successUpdated = orderStateProbe.getOrder(successOrder.getId());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isEqualTo(1);
            softly.assertThat(result.failureReasons()).containsEntry("NotFoundException", 1);
            softly.assertThat(failedUpdated.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
            softly.assertThat(successUpdated.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED);
        });
    }

    @DisplayName("픽업 완료와 만료 처리 경합 시 최종 상태는 단일하게 유지된다")
    @Test
    void pickupComplete_and_expireProcess_race_keepsSingleTerminalState() throws InterruptedException {
        OrderTestHelper.OrderFixture fixture = orderHelper.createReadyStockPaidOrder("픽업 경합 테스트 상품", 53000L);
        Order order = fixture.order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        markPickupReadyWithExpiredDeadline(order.getId());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Throwable> pickupError = new AtomicReference<>();
        AtomicReference<Throwable> expireError = new AtomicReference<>();

        try {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    orderPickupService.confirmPickup(order.getId(), 1L);
                } catch (Throwable t) {
                    pickupError.set(t);
                }
            });

            executor.submit(() -> {
                try {
                    startLatch.await();
                    pickupExpireProcessor.process(order.getId(), LocalDateTime.now(clock));
                } catch (Throwable t) {
                    expireError.set(t);
                }
            });

            startLatch.countDown();
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        Order updated = orderStateProbe.getOrder(order.getId());

        assertSoftly(softly -> {
            if (updated.getStatus() == OrderStatus.PICKUP_EXPIRED) {
                softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED);
                softly.assertThat(orderStateProbe.refundCount()).isEqualTo(1L);
            } else {
                softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
            }
            if (pickupError.get() != null) {
                softly.assertThat(pickupError.get()).isInstanceOf(RuntimeException.class);
            }
            if (expireError.get() != null) {
                softly.assertThat(expireError.get()).isInstanceOf(RuntimeException.class);
            }
        });
    }

    @DisplayName("현재보다 이르거나 같은 픽업 마감은 주문 상태를 바꾸지 않고 거절한다")
    @Test
    void markPickupReady_nonFutureDeadline_rejected() {
        Order order = orderHelper.createReadyStockPaidOrder("잘못된 픽업 마감 상품", 30_000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);

        assertThatThrownBy(() -> orderPickupService.markPickupReady(
                order.getId(), LocalDateTime.now(clock), ADMIN_ID))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("현재보다 이후");
        assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                .isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
    }

    private void markPickupReadyWithExpiredDeadline(Long orderId) {
        orderPickupService.markPickupReady(orderId, LocalDateTime.now(clock).plusHours(1), ADMIN_ID);
        var fulfillment = fulfillmentPort.findByOrderId(orderId).orElseThrow();
        fulfillment.setPickupDeadline(LocalDateTime.now(clock).minusHours(1));
        fulfillmentPort.save(fulfillment);
    }
}
