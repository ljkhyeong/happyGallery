package com.personal.happygallery.app.order;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired PickupExpireBatchService pickupExpireBatchService;
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

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // FK 삭제 순서: refunds → fulfillments → order_items → orders → inventory → products
        refundRepository.deleteAll();
        fulfillmentRepository.deleteAll();
        orderApprovalHistoryRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        productRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.4): 픽업 마감 초과 → 자동환불 + 재고 복구
    // -----------------------------------------------------------------------

    @Test
    void expirePickups_expiredDeadline_refundsAndRestoresInventory() {
        Product product = productRepository.save(new Product("픽업 테스트 상품", ProductType.READY_STOCK, 50000L));
        inventoryRepository.save(new Inventory(product, 1));

        // 주문 생성 → 재고 차감
        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 50000L)));
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(0);

        // 승인 → APPROVED_FULFILLMENT_PENDING
        orderApprovalService.approve(order.getId());

        // 픽업 준비 완료 (마감 시각: 과거)
        LocalDateTime pastDeadline = LocalDateTime.now(clock).minusHours(1);
        orderPickupService.markPickupReady(order.getId(), pastDeadline);

        Order afterReady = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(afterReady.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);

        // 배치 실행
        BatchResult result = pickupExpireBatchService.expirePickups();
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isZero();

        // 상태 확인
        Order expired = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED_REFUNDED);

        // 재고 복구 확인
        assertThat(inventoryRepository.findByProductId(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);

        // 환불 기록 확인
        assertThat(refundRepository.findAll()).hasSize(1);
        assertThat(refundRepository.findAll().get(0).getOrderId()).isEqualTo(order.getId());

        // Fulfillment 상태 동기화 확인
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED_REFUNDED);
    }

    // -----------------------------------------------------------------------
    // 마감 미경과 → 배치가 처리하지 않음
    // -----------------------------------------------------------------------

    @Test
    void expirePickups_futureDeadline_notExpired() {
        Product product = productRepository.save(new Product("미만료 픽업 상품", ProductType.READY_STOCK, 30000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 30000L)));

        orderApprovalService.approve(order.getId());

        // 픽업 준비 완료 (마감 시각: 미래)
        LocalDateTime futureDeadline = LocalDateTime.now(clock).plusDays(1);
        orderPickupService.markPickupReady(order.getId(), futureDeadline);

        // 배치 실행 → 0건 처리
        BatchResult result = pickupExpireBatchService.expirePickups();
        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isZero();

        // 상태 유지 확인
        Order unchanged = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
    }

    @Test
    void expirePickups_adminApi_returnsBatchResponse() throws Exception {
        Product product = productRepository.save(new Product("픽업 API 테스트 상품", ProductType.READY_STOCK, 45000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 45000L)));
        orderApprovalService.approve(order.getId());
        orderPickupService.markPickupReady(order.getId(), LocalDateTime.now(clock).minusMinutes(30));

        mockMvc.perform(post("/admin/orders/expire-pickups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.failureReasons").isMap());

        Order expired = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.PICKUP_EXPIRED_REFUNDED);
    }
}
