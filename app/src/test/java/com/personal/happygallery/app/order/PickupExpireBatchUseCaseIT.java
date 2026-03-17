package com.personal.happygallery.app.order;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.support.OrderTestHelper;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static com.personal.happygallery.support.TestDataCleaner.clearOrderData;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [UseCaseIT] §8.4 픽업 만료 배치 검증.
 *
 * <p>Proof (§8.4 DoD): 배치 실행 시 픽업 만료 주문이 환불되고 재고가 복구됨.
 */
@UseCaseIT
class PickupExpireBatchUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired PickupExpireBatchUseCase pickupExpireBatchService;
    @Autowired PickupExpireProcessor pickupExpireProcessor;
    @Autowired OrderPickupService orderPickupService;
    @Autowired OrderApprovalService orderApprovalService;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    @Autowired FulfillmentRepository fulfillmentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired Clock clock;
    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        orderHelper = new OrderTestHelper(
                productRepository,
                inventoryRepository,
                orderRepository,
                orderItemRepository,
                orderService,
                clock);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        clearOrderData(
                refundRepository,
                fulfillmentRepository,
                orderApprovalHistoryRepository,
                orderItemRepository,
                orderRepository,
                inventoryRepository,
                productRepository);
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.4): 픽업 마감 초과 → 자동환불 + 재고 복구
    // -----------------------------------------------------------------------

    @DisplayName("픽업 기한이 지난 주문은 환불되고 재고가 복구된다")
    @Test
    void expirePickups_expiredDeadline_refundsAndRestoresInventory() {
        OrderTestHelper.OrderFixture fixture = orderHelper.createReadyStockPaidOrder("픽업 테스트 상품", 50000L);
        Order order = fixture.order();
        assertThat(inventoryRepository.findByProductId(fixture.product().getId()).orElseThrow().getQuantity()).isEqualTo(0);

        // 승인 → APPROVED_FULFILLMENT_PENDING
        orderApprovalService.approve(order.getId());

        // 픽업 준비 완료 (마감 시각: 과거)
        LocalDateTime pastDeadline = LocalDateTime.now(clock).minusHours(1);
        orderPickupService.markPickupReady(order.getId(), pastDeadline);

        Order afterReady = orderRepository.findById(order.getId()).orElseThrow();

        // 배치 실행
        BatchResult result = pickupExpireBatchService.expirePickups();

        // 상태 확인
        Order expired = orderRepository.findById(order.getId()).orElseThrow();

        // 재고 복구 확인
        int restoredQuantity = inventoryRepository.findByProductId(fixture.product().getId()).orElseThrow().getQuantity();

        // 환불 기록 확인
        var refunds = refundRepository.findAll();

        assertSoftly(softly -> {
            softly.assertThat(afterReady.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(expired.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED);
            softly.assertThat(restoredQuantity).isEqualTo(1);
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refunds.get(0).getOrderId()).isEqualTo(order.getId());
        });
    }

    // -----------------------------------------------------------------------
    // 마감 미경과 → 배치가 처리하지 않음
    // -----------------------------------------------------------------------

    @DisplayName("픽업 기한이 남은 주문은 만료 처리되지 않는다")
    @Test
    void expirePickups_futureDeadline_notExpired() {
        Order order = orderHelper.createReadyStockPaidOrder("미만료 픽업 상품", 30000L).order();

        orderApprovalService.approve(order.getId());

        // 픽업 준비 완료 (마감 시각: 미래)
        LocalDateTime futureDeadline = LocalDateTime.now(clock).plusDays(1);
        orderPickupService.markPickupReady(order.getId(), futureDeadline);

        // 배치 실행 → 0건 처리
        BatchResult result = pickupExpireBatchService.expirePickups();

        // 상태 유지 확인
        Order unchanged = orderRepository.findById(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(0);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
        });
    }

    @DisplayName("픽업 만료 배치 관리자 API는 배치 결과를 반환한다")
    @Test
    void expirePickups_adminApi_returnsBatchResponse() throws Exception {
        Order order = orderHelper.createReadyStockPaidOrder("픽업 API 테스트 상품", 45000L).order();
        orderApprovalService.approve(order.getId());
        orderPickupService.markPickupReady(order.getId(), LocalDateTime.now(clock).minusMinutes(30));

        mockMvc.perform(post("/admin/orders/expire-pickups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.failureReasons").isMap());

        Order expired = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED);
    }

    @DisplayName("픽업 만료 배치에서 한 건이 실패해도 다음 주문을 계속 처리하고 실패를 집계한다")
    @Test
    void expirePickups_whenOneOrderFails_continuesNextOrderAndCountsFailure() {
        OrderTestHelper.OrderFixture failedFixture = orderHelper.createReadyStockPaidOrder("픽업 만료 실패 상품", 41000L);
        OrderTestHelper.OrderFixture successFixture = orderHelper.createReadyStockPaidOrder("픽업 만료 성공 상품", 42000L);
        Order failedOrder = failedFixture.order();
        Order successOrder = successFixture.order();

        orderApprovalService.approve(failedOrder.getId());
        orderApprovalService.approve(successOrder.getId());

        LocalDateTime pastDeadline = LocalDateTime.now(clock).minusHours(1);
        orderPickupService.markPickupReady(failedOrder.getId(), pastDeadline);
        orderPickupService.markPickupReady(successOrder.getId(), pastDeadline);

        // 실패 케이스 유도: 재고 레코드가 사라진 상태에서 복구 시도하면 NotFoundException 발생
        inventoryRepository.deleteById(failedFixture.product().getId());

        BatchResult result = pickupExpireBatchService.expirePickups();

        Order failedUpdated = orderRepository.findById(failedOrder.getId()).orElseThrow();
        Order successUpdated = orderRepository.findById(successOrder.getId()).orElseThrow();
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
        orderApprovalService.approve(order.getId());
        LocalDateTime pastDeadline = LocalDateTime.now(clock).minusMinutes(1);
        orderPickupService.markPickupReady(order.getId(), pastDeadline);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<Throwable> pickupError = new AtomicReference<>();
        AtomicReference<Throwable> expireError = new AtomicReference<>();

        try {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    orderPickupService.confirmPickup(order.getId());
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

        Order updated = orderRepository.findById(order.getId()).orElseThrow();

        if (updated.getStatus() == OrderStatus.PICKUP_EXPIRED) {
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED);
            assertThat(refundRepository.count()).isEqualTo(1L);
        } else {
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
        }

        if (pickupError.get() != null) {
            assertThat(pickupError.get()).isInstanceOf(RuntimeException.class);
        }
        if (expireError.get() != null) {
            assertThat(expireError.get()).isInstanceOf(RuntimeException.class);
        }
    }
}
