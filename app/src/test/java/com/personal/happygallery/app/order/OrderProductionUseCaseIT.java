package com.personal.happygallery.app.order;

import com.personal.happygallery.common.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
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
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [UseCaseIT] §8.3 예약 제작 주문 검증.
 *
 * <p>Proof (PLAN.md §8.3): 제작 시작 상태에서 취소 요청 시 "환불 불가"로 처리됨.
 */
@UseCaseIT
class OrderProductionUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    @Autowired FulfillmentRepository fulfillmentRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired ProductRepository productRepository;
    @Autowired InventoryRepository inventoryRepository;
    @Autowired OrderApprovalService orderApprovalService;
    @Autowired OrderProductionService orderProductionService;
    @Autowired OrderService orderService;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        refundRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        orderApprovalHistoryRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    // -----------------------------------------------------------------------
    // MADE_TO_ORDER 승인 → IN_PRODUCTION + Fulfillment 생성
    // -----------------------------------------------------------------------

    @DisplayName("주문제작 상품 주문 승인 시 IN_PRODUCTION으로 전이되고 Fulfillment가 생성된다")
    @Test
    void approve_madeToOrder_transitionsToInProductionAndCreatesFulfillment() {
        Product product = productRepository.save(
                new Product("예약 제작 상품", ProductType.MADE_TO_ORDER, 200000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 200000L)));

        orderApprovalService.approve(order.getId());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);

        // Fulfillment 자동 생성 확인
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
    }

    // -----------------------------------------------------------------------
    // READY_STOCK 승인 → 기존 흐름 유지 (Fulfillment 미생성)
    // -----------------------------------------------------------------------

    @DisplayName("기성품 주문 승인 시 APPROVED_FULFILLMENT_PENDING 상태를 유지한다")
    @Test
    void approve_readyStock_remainsApprovedFulfillmentPending() {
        Product product = productRepository.save(
                new Product("기성품", ProductType.READY_STOCK, 50000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 50000L)));

        orderApprovalService.approve(order.getId());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
        assertThat(fulfillmentRepository.findByOrderId(order.getId())).isEmpty();
    }

    // -----------------------------------------------------------------------
    // 예상 출고일 설정
    // -----------------------------------------------------------------------

    @DisplayName("예상 출고일 설정 시 Fulfillment의 출고일이 갱신된다")
    @Test
    void setExpectedShipDate_updatesShipDateOnFulfillment() {
        Product product = productRepository.save(
                new Product("출고일 설정 상품", ProductType.MADE_TO_ORDER, 150000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 150000L)));
        orderApprovalService.approve(order.getId());

        LocalDate shipDate = LocalDate.of(2026, 4, 15);
        orderProductionService.setExpectedShipDate(order.getId(), shipDate);

        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getExpectedShipDate()).isEqualTo(shipDate);
    }

    // -----------------------------------------------------------------------
    // DELAY_REQUESTED 전환 (고객 동의)
    // -----------------------------------------------------------------------

    @DisplayName("배송 지연 요청 시 주문 상태가 DELAY_REQUESTED로 전이된다")
    @Test
    void requestDelay_transitionsToDelayRequested() {
        Product product = productRepository.save(
                new Product("지연 상품", ProductType.MADE_TO_ORDER, 180000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 180000L)));
        orderApprovalService.approve(order.getId());

        orderProductionService.requestDelay(order.getId());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELAY_REQUESTED);

        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getStatus()).isEqualTo(OrderStatus.DELAY_REQUESTED);
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.DELAY);
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.3): IN_PRODUCTION 상태에서 reject → 422 (환불 불가)
    // -----------------------------------------------------------------------

    @DisplayName("IN_PRODUCTION 상태에서 거절하면 제작 환불 불가 예외가 발생한다")
    @Test
    void reject_inProduction_throwsProductionRefundNotAllowed() {
        Product product = productRepository.save(
                new Product("제작 취소 불가 상품", ProductType.MADE_TO_ORDER, 250000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 250000L)));
        orderApprovalService.approve(order.getId());

        // IN_PRODUCTION 상태에서 reject → ProductionRefundNotAllowedException
        assertThatThrownBy(() -> orderApprovalService.reject(order.getId()))
                .isInstanceOf(ProductionRefundNotAllowedException.class);

        // 상태 변경 없음 확인
        Order unchanged = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
    }

    // -----------------------------------------------------------------------
    // 제작 완료 → APPROVED_FULFILLMENT_PENDING → PICKUP_READY 전체 흐름
    // -----------------------------------------------------------------------

    @Test
    void completeProduction_transitionsToApprovedFulfillmentPending() {
        Product product = productRepository.save(
                new Product("제작완료 상품", ProductType.MADE_TO_ORDER, 200000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 200000L)));
        orderApprovalService.approve(order.getId());

        // IN_PRODUCTION → completeProduction → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), 1L);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);

        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);

        // 이력: APPROVE + PRODUCTION_COMPLETE
        assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                .extracting("decision")
                .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.PRODUCTION_COMPLETE);

        // adminId 기록 확인
        var histories = orderApprovalHistoryRepository.findByOrderId(order.getId());
        assertThat(histories.get(1).getDecidedByAdminId()).isEqualTo(1L);
    }

    @Test
    void completeProduction_fromDelayRequested_alsoWorks() {
        Product product = productRepository.save(
                new Product("지연 후 제작완료 상품", ProductType.MADE_TO_ORDER, 180000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 180000L)));
        orderApprovalService.approve(order.getId());
        orderProductionService.requestDelay(order.getId());

        // DELAY_REQUESTED → completeProduction → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), null);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
    }

    @Test
    void completeProduction_thenPickupReady_fullFlow() throws Exception {
        Product product = productRepository.save(
                new Product("제작→픽업 전체 흐름 상품", ProductType.MADE_TO_ORDER, 250000L));
        inventoryRepository.save(new Inventory(product, 1));

        Order order = orderService.createPaidOrder(null,
                java.util.List.of(new OrderService.OrderItemRequest(product.getId(), 1, 250000L)));

        // 승인 → IN_PRODUCTION
        orderApprovalService.approve(order.getId());
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.IN_PRODUCTION);

        // 제작 완료 → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), 1L);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);

        // 픽업 준비 → PICKUP_READY (기존 흐름과 연결 확인)
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/admin/orders/{id}/prepare-pickup", order.getId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pickupDeadlineAt\":\"2026-04-01T18:00:00\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        Order final_ = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(final_.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
    }
}
