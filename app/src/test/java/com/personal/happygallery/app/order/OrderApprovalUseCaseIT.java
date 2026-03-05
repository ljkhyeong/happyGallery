package com.personal.happygallery.app.order;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.common.error.AlreadyRefundedException;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [UseCaseIT] §8.2 주문 승인 모델 검증.
 *
 * <p>Proof (PLAN.md §8.2): 24h 경과 케이스에서 자동환불되고 승인 시도는 409.
 */
@UseCaseIT
class OrderApprovalUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired OrderApprovalService orderApprovalService;
    @Autowired OrderAutoRefundBatchService orderAutoRefundBatchService;
    @Autowired OrderService orderService;
    @Autowired Clock clock;
    @MockitoBean NotificationService notificationService;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // FK 삭제 순서: refunds(order_id) → order_items → orders → inventory → products
        refundRepository.deleteAll();
        orderApprovalHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Proof: 승인 → APPROVED_FULFILLMENT_PENDING
    // -----------------------------------------------------------------------

    @Test
    void approve_transitionsToApprovedFulfillmentPending() throws Exception {
        Product product = productRepository.save(new Product("테스트 상품", ProductType.READY_STOCK, 50000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 50000L)));

        mockMvc.perform(post("/admin/orders/{id}/approve", order.getId()))
                .andExpect(status().isOk());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.APPROVE);
    }

    // -----------------------------------------------------------------------
    // Proof: 거절 → REJECTED_REFUNDED + 재고 복구 + 환불 기록
    // -----------------------------------------------------------------------

    @Test
    void reject_refundsAndRestoresInventory() throws Exception {
        Product product = productRepository.save(new Product("거절 테스트 상품", ProductType.READY_STOCK, 30000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 30000L)));

        // 재고 차감 확인
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(0);

        mockMvc.perform(post("/admin/orders/{id}/reject", order.getId()))
                .andExpect(status().isOk());

        // 주문 상태 확인
        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.REJECTED_REFUNDED);
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.REJECT);

        // 재고 복구 확인
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);

        // 환불 기록 확인
        assertThat(refundRepository.findAll()).hasSize(1);
        assertThat(refundRepository.findAll().get(0).getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void approve_twice_keepsSingleTransitionAndHistory() {
        Product product = productRepository.save(new Product("중복 승인 테스트 상품", ProductType.READY_STOCK, 50000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 50000L)));

        orderApprovalService.approve(order.getId());

        assertThatThrownBy(() -> orderApprovalService.approve(order.getId()))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("승인 대기 상태의 주문만 처리할 수 있습니다.");

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.APPROVE);
    }

    @Test
    void reject_twice_keepsSingleTransitionAndHistory() {
        Product product = productRepository.save(new Product("중복 거절 테스트 상품", ProductType.READY_STOCK, 30000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 30000L)));

        orderApprovalService.reject(order.getId());

        assertThatThrownBy(() -> orderApprovalService.reject(order.getId()))
                .isInstanceOf(AlreadyRefundedException.class);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.REJECTED_REFUNDED);
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.REJECT);
        assertThat(refundRepository.findAll()).hasSize(1);
    }

    @Test
    void reject_afterApprove_returns400AndDoesNotRefund() {
        Product product = productRepository.save(new Product("승인 후 거절 테스트 상품", ProductType.READY_STOCK, 40000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 40000L)));

        orderApprovalService.approve(order.getId());

        assertThatThrownBy(() -> orderApprovalService.reject(order.getId()))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("승인 대기 상태의 주문만 처리할 수 있습니다.");

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.APPROVE);
        assertThat(refundRepository.findAll()).isEmpty();
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(0);
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.2): 24h 경과 → 자동환불 → AUTO_REFUNDED_TIMEOUT + 재고 복구
    // -----------------------------------------------------------------------

    @Test
    void autoRefund_expiredOrder_transitionsAndRestoresInventory() {
        Product product = productRepository.save(new Product("자동환불 상품", ProductType.READY_STOCK, 40000L));
        inventoryRepository.save(new Inventory(product, 1));

        // 마감이 이미 지난 주문을 직접 생성 (Clock 조작 없이 과거 시각 설정)
        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderRepository.save(new Order(null, 40000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order, product.getId(), 1, 40000L));
        inventoryRepository.findByProductId(product.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        // 배치 실행
        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();

        // 상태 확인
        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.AUTO_REFUNDED_TIMEOUT);

        // 재고 복구 확인
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);

        // 환불 기록 확인
        assertThat(refundRepository.findAll()).hasSize(1);
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.2): 자동환불 이후 승인 시도 → 409
    // -----------------------------------------------------------------------

    @Test
    void approve_afterAutoRefund_throws409() throws Exception {
        Product product = productRepository.save(new Product("409 테스트 상품", ProductType.READY_STOCK, 60000L));
        inventoryRepository.save(new Inventory(product, 1));

        // 마감이 지난 주문 직접 생성
        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderRepository.save(new Order(null, 60000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order, product.getId(), 1, 60000L));
        inventoryRepository.findByProductId(product.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        // 배치 실행 → AUTO_REFUNDED_TIMEOUT
        orderAutoRefundBatchService.autoRefundExpired();

        // 승인 시도 → AlreadyRefundedException (409)
        assertThatThrownBy(() -> orderApprovalService.approve(order.getId()))
                .isInstanceOf(AlreadyRefundedException.class);
    }

    // -----------------------------------------------------------------------
    // Proof: 자동환불 이후 승인 HTTP 요청 → 409 응답
    // -----------------------------------------------------------------------

    @Test
    void approve_afterAutoRefund_returns409() throws Exception {
        Product product = productRepository.save(new Product("HTTP 409 상품", ProductType.READY_STOCK, 70000L));
        inventoryRepository.save(new Inventory(product, 1));

        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderRepository.save(new Order(null, 70000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order, product.getId(), 1, 70000L));
        inventoryRepository.findByProductId(product.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        orderAutoRefundBatchService.autoRefundExpired();

        mockMvc.perform(post("/admin/orders/{id}/approve", order.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void autoRefund_whenOneOrderFails_continuesNextOrderAndCountsFailure() {
        Product failedProduct = productRepository.save(new Product("자동환불 실패 상품", ProductType.READY_STOCK, 45000L));
        Product successProduct = productRepository.save(new Product("자동환불 성공 상품", ProductType.READY_STOCK, 55000L));
        inventoryRepository.save(new Inventory(failedProduct, 1));
        inventoryRepository.save(new Inventory(successProduct, 1));

        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);

        Order failedOrder = orderRepository.save(new Order(null, 45000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(failedOrder, failedProduct.getId(), 1, 45000L));
        inventoryRepository.findByProductId(failedProduct.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        Order successOrder = orderRepository.save(new Order(null, 55000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(successOrder, successProduct.getId(), 1, 55000L));
        inventoryRepository.findByProductId(successProduct.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        doThrow(new RuntimeException("알림 전송 실패"))
                .doNothing()
                .when(notificationService)
                .notifyByGuestId(any(), any());

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(1);
        assertThat(result.failureReasons()).containsEntry("RuntimeException", 1);

        Order failedUpdated = orderRepository.findById(failedOrder.getId()).orElseThrow();
        Order successUpdated = orderRepository.findById(successOrder.getId()).orElseThrow();

        assertThat(failedUpdated.getStatus()).isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
        assertThat(successUpdated.getStatus()).isEqualTo(OrderStatus.AUTO_REFUNDED_TIMEOUT);
    }
}
