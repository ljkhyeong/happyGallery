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
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
    @Autowired FulfillmentRepository fulfillmentRepository;
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
        refundRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        orderApprovalHistoryRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    // -----------------------------------------------------------------------
    // Proof: 승인 → APPROVED_FULFILLMENT_PENDING
    // -----------------------------------------------------------------------

    @DisplayName("주문 승인 시 APPROVED_FULFILLMENT_PENDING 상태로 전이된다")
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

    @DisplayName("주문 거절 시 환불 처리와 재고 복구가 수행된다")
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

    @DisplayName("주문을 두 번 승인해도 상태 전이와 이력은 한 번만 기록된다")
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

    @DisplayName("주문을 두 번 거절해도 상태 전이와 이력은 한 번만 기록된다")
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

    @DisplayName("승인 후 거절을 시도하면 400을 반환하고 환불되지 않는다")
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

    @DisplayName("24시간 초과 주문 자동환불 시 상태 전이와 재고 복구가 수행된다")
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

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();
        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.AUTO_REFUNDED_TIMEOUT);
            softly.assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);
            softly.assertThat(refundRepository.findAll()).hasSize(1);
        });
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.2): 자동환불 이후 승인 시도 → 409
    // -----------------------------------------------------------------------

    @DisplayName("자동환불된 주문을 승인하면 409 예외가 발생한다")
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

    @DisplayName("자동환불된 주문을 승인하면 409를 반환한다")
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

    // -----------------------------------------------------------------------
    // Proof: MADE_TO_ORDER 승인 후(IN_PRODUCTION)는 24h 자동환불 배치 대상이 아니다.
    // -----------------------------------------------------------------------

    @DisplayName("IN_PRODUCTION 상태의 주문제작 주문은 자동환불 배치에서 제외된다")
    @Test
    void autoRefund_inProductionMadeToOrder_notProcessed() {
        Product product = productRepository.save(new Product("제작중 자동환불 제외 상품", ProductType.MADE_TO_ORDER, 80000L));
        inventoryRepository.save(new Inventory(product, 1));

        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderRepository.save(new Order(null, 80000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order, product.getId(), 1, 80000L));
        inventoryRepository.findByProductId(product.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        // MADE_TO_ORDER 승인 → IN_PRODUCTION
        orderApprovalService.approve(order.getId());

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();

        assertThat(result.successCount()).isZero();
        assertThat(result.failureCount()).isZero();

        Order unchanged = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        assertThat(refundRepository.findAll()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Proof: 자동환불 이후 거절 시도 HTTP 요청 → 409 응답
    // -----------------------------------------------------------------------

    @DisplayName("자동환불된 주문을 거절하면 409를 반환한다")
    @Test
    void reject_afterAutoRefund_returns409() throws Exception {
        Product product = productRepository.save(new Product("자동환불 후 거절 409 상품", ProductType.READY_STOCK, 72000L));
        inventoryRepository.save(new Inventory(product, 1));

        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);
        Order order = orderRepository.save(new Order(null, 72000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order, product.getId(), 1, 72000L));
        inventoryRepository.findByProductId(product.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        orderAutoRefundBatchService.autoRefundExpired();

        mockMvc.perform(post("/admin/orders/{id}/reject", order.getId()))
                .andExpect(status().isConflict());
    }

    @DisplayName("자동환불 알림이 실패해도 환불 처리는 롤백되지 않는다")
    @Test
    void autoRefund_notificationFailure_doesNotRollbackRefund() {
        Product product1 = productRepository.save(new Product("알림실패 상품1", ProductType.READY_STOCK, 45000L));
        Product product2 = productRepository.save(new Product("알림실패 상품2", ProductType.READY_STOCK, 55000L));
        inventoryRepository.save(new Inventory(product1, 1));
        inventoryRepository.save(new Inventory(product2, 1));

        LocalDateTime paidAt = LocalDateTime.now(clock).minusHours(25);

        Order order1 = orderRepository.save(new Order(null, 45000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order1, product1.getId(), 1, 45000L));
        inventoryRepository.findByProductId(product1.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        Order order2 = orderRepository.save(new Order(null, 55000L, paidAt, paidAt.plusHours(24)));
        orderItemRepository.save(new OrderItem(order2, product2.getId(), 1, 55000L));
        inventoryRepository.findByProductId(product2.getId()).ifPresent(inv -> {
            inv.deduct(1);
            inventoryRepository.save(inv);
        });

        // 첫 번째 주문의 알림만 실패
        doThrow(new RuntimeException("알림 전송 실패"))
                .doNothing()
                .when(notificationService)
                .notifyByGuestId(any(), any());

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();

        Order updated1 = orderRepository.findById(order1.getId()).orElseThrow();
        Order updated2 = orderRepository.findById(order2.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(2);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(updated1.getStatus()).isEqualTo(OrderStatus.AUTO_REFUNDED_TIMEOUT);
            softly.assertThat(updated2.getStatus()).isEqualTo(OrderStatus.AUTO_REFUNDED_TIMEOUT);
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 주문 환불 FAILED 건이 admin API에서 orderId와 함께 조회된다
    // -----------------------------------------------------------------------

    @DisplayName("주문 환불 실패 건이 admin 환불 실패 목록에서 orderId와 함께 조회된다")
    @Test
    void listFailedRefunds_orderRefund_returnsOrderIdWithoutNpe() throws Exception {
        Product product = productRepository.save(new Product("주문환불실패 상품", ProductType.READY_STOCK, 90000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 90000L)));

        // 주문 환불 FAILED 직접 생성 (booking 없는 refund)
        var refund = new com.personal.happygallery.domain.booking.Refund(order.getId(), 90000L);
        refund.markFailed("PG 점검중");
        refundRepository.save(refund);

        mockMvc.perform(get("/admin/refunds/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].refundId").value(refund.getId()))
                .andExpect(jsonPath("$[0].bookingId", nullValue()))
                .andExpect(jsonPath("$[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$[0].amount").value(90000))
                .andExpect(jsonPath("$[0].failReason").value("PG 점검중"));
    }
}
