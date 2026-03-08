package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.InventoryNotEnoughException;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.UseCaseIT;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.TestDataCleaner.clearOrderData;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * [UseCaseIT] 단일 재고 동시 주문 동시성 검증.
 *
 * <p>Proof (§12.1 DoD): quantity=1 상품에 5개 스레드가 동시에 주문할 때
 * PESSIMISTIC_WRITE 락으로 정확히 1건만 성공하고 나머지는 재고 부족으로 실패한다.
 */
@UseCaseIT
class ConcurrentOrderUseCaseIT {

    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    @Autowired FulfillmentRepository fulfillmentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        orderHelper = new OrderTestHelper(
                productRepository,
                inventoryRepository,
                orderRepository,
                orderItemRepository,
                orderService);
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
    // Proof (§12.1 ★★★): quantity=1 → 동시 5건 중 1건만 성공
    // -----------------------------------------------------------------------

    @DisplayName("수량 1 재고에서 동시 주문을 시도하면 1건만 성공한다")
    @Test
    void concurrentOrder_quantity1_onlyOneSucceeds() throws InterruptedException {
        Product product = orderHelper.createReadyStockProduct("단일 작품(동시성)", 50000L, 1);

        int threadCount = 5;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures  = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    orderService.createPaidOrder(null,
                            List.of(new OrderService.OrderItemRequest(product.getId(), 1, 50000L)));
                    successes.incrementAndGet();
                } catch (InventoryNotEnoughException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        startLatch.countDown();
        exec.shutdown();
        exec.awaitTermination(15, TimeUnit.SECONDS);

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threadCount - 1);

        // 최종 재고 0 확인 (음수로 내려가지 않음)
        int remaining = inventoryRepository.findByProductId(product.getId())
                .orElseThrow().getQuantity();
        assertThat(remaining).isEqualTo(0);
    }
}
