package com.personal.happygallery.app.order;

import com.personal.happygallery.app.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.app.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.app.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.app.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.common.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.order.FulfillmentRepository;
import com.personal.happygallery.infra.order.OrderItemRepository;
import com.personal.happygallery.infra.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static com.personal.happygallery.support.TestDataCleaner.clearOrderData;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [UseCaseIT] §8.3 예약 제작 주문 검증.
 *
 * <p>Proof (docs/PRD/0001_기준_스펙/spec.md §8.3): 제작 시작 상태에서 취소 요청 시 "환불 불가"로 처리됨.
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
    @Autowired OrderApprovalUseCase orderApprovalService;
    @Autowired OrderProductionUseCase orderProductionService;
    @Autowired OrderPickupUseCase orderPickupService;
    @Autowired OrderShippingUseCase orderShippingService;
    @Autowired OrderService orderService;
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
    // MADE_TO_ORDER 승인 → IN_PRODUCTION + Fulfillment 생성
    // -----------------------------------------------------------------------

    @DisplayName("주문제작 상품 주문 승인 시 IN_PRODUCTION으로 전이되고 Fulfillment가 생성된다")
    @Test
    void approve_madeToOrder_transitionsToInProductionAndCreatesFulfillment() {
        Order order = orderHelper.createMadeToOrderPaidOrder("예약 제작 상품", 200000L).order();

        orderApprovalService.approve(order.getId());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(fulfillment.getType()).isEqualTo(FulfillmentType.SHIPPING);
        });
    }

    // -----------------------------------------------------------------------
    // READY_STOCK 승인 → 기존 흐름 유지 (Fulfillment 미생성)
    // -----------------------------------------------------------------------

    @DisplayName("기성품 주문 승인 시 APPROVED_FULFILLMENT_PENDING 상태를 유지한다")
    @Test
    void approve_readyStock_remainsApprovedFulfillmentPending() {
        Order order = orderHelper.createReadyStockPaidOrder("기성품", 50000L).order();

        orderApprovalService.approve(order.getId());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(fulfillmentRepository.findByOrderId(order.getId())).isEmpty();
        });
    }

    // -----------------------------------------------------------------------
    // 예상 출고일 설정
    // -----------------------------------------------------------------------

    @DisplayName("예상 출고일 설정 시 Fulfillment의 출고일이 갱신된다")
    @Test
    void setExpectedShipDate_updatesShipDateOnFulfillment() {
        Order order = orderHelper.createMadeToOrderPaidOrder("출고일 설정 상품", 150000L).order();
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
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 상품", 180000L).order();
        orderApprovalService.approve(order.getId());

        orderProductionService.requestDelay(order.getId());

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELAY_REQUESTED);
            softly.assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.DELAY);
        });
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.3): IN_PRODUCTION 상태에서 reject → 422 (환불 불가)
    // -----------------------------------------------------------------------

    @DisplayName("IN_PRODUCTION 상태에서 거절하면 제작 환불 불가 예외가 발생한다")
    @Test
    void reject_inProduction_throwsProductionRefundNotAllowed() {
        Order order = orderHelper.createMadeToOrderPaidOrder("제작 취소 불가 상품", 250000L).order();
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

    @DisplayName("제작 완료 처리 시 APPROVED_FULFILLMENT_PENDING 상태로 전이된다")
    @Test
    void completeProduction_transitionsToApprovedFulfillmentPending() {
        Order order = orderHelper.createMadeToOrderPaidOrder("제작완료 상품", 200000L).order();
        orderApprovalService.approve(order.getId());

        // IN_PRODUCTION → completeProduction → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), 1L);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        var histories = orderApprovalHistoryRepository.findByOrderId(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.PRODUCTION_COMPLETE);
            softly.assertThat(histories.get(1).getDecidedByAdminId()).isEqualTo(1L);
        });
    }

    @DisplayName("DELAY_REQUESTED 상태에서도 제작 완료 처리가 가능하다")
    @Test
    void completeProduction_fromDelayRequested_alsoWorks() {
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 후 제작완료 상품", 180000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.requestDelay(order.getId());

        // DELAY_REQUESTED → completeProduction → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), null);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
    }

    // -----------------------------------------------------------------------
    // DELAY_REQUESTED → resumeProduction → IN_PRODUCTION
    // -----------------------------------------------------------------------

    @DisplayName("지연 요청 상태에서 제작을 재개하면 IN_PRODUCTION으로 전이된다")
    @Test
    void resumeProduction_fromDelayRequested_transitionsToInProduction() {
        Order order = orderHelper.createMadeToOrderPaidOrder("재개 상품", 180000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.requestDelay(order.getId());

        orderProductionService.resumeProduction(order.getId(), 1L);

        Order updated = orderRepository.findById(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(orderApprovalHistoryRepository.findByOrderId(order.getId()))
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.RESUME_PRODUCTION);
        });
    }

    // -----------------------------------------------------------------------
    // resume-production HTTP 엔드포인트 검증
    // -----------------------------------------------------------------------

    @DisplayName("POST /admin/orders/{id}/resume-production HTTP 호출 시 200과 IN_PRODUCTION 상태를 반환한다")
    @Test
    void resumeProduction_httpEndpoint_returns200WithInProductionStatus() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("HTTP 재개 상품", 180000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.requestDelay(order.getId());

        String body = mockMvc.perform(post("/admin/orders/{id}/resume-production", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(body).contains("\"status\":\"IN_PRODUCTION\"");
            softly.assertThat(body).contains("\"orderId\":" + order.getId());
        });
    }

    @DisplayName("PAID_APPROVAL_PENDING 상태에서 resume-production 호출 시 400을 반환한다")
    @Test
    void resumeProduction_httpEndpoint_invalidState_returns400() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("잘못된 상태 재개", 180000L).order();

        mockMvc.perform(post("/admin/orders/{id}/resume-production", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Fulfillment 단일성: MADE_TO_ORDER → completeProduction → markPickupReady
    // -----------------------------------------------------------------------

    @DisplayName("MADE_TO_ORDER 주문의 제작 완료 후 픽업 준비 시 fulfillment는 1건이다")
    @Test
    void madeToOrder_completeProduction_thenPickupReady_singleFulfillment() {
        Order order = orderHelper.createMadeToOrderPaidOrder("단일성 상품", 200000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.completeProduction(order.getId(), 1L);

        orderPickupService.markPickupReady(order.getId(),
                LocalDateTime.of(2026, 4, 1, 18, 0));

        var fulfillments = fulfillmentRepository.findAll().stream()
                .filter(f -> f.getOrderId().equals(order.getId()))
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(fulfillments).hasSize(1);
            softly.assertThat(fulfillments.get(0).getType())
                    .isEqualTo(FulfillmentType.PICKUP);
            softly.assertThat(fulfillments.get(0).getPickupDeadlineAt()).isNotNull();
        });
    }

    @DisplayName("제작 완료 후 픽업 준비까지 전체 흐름이 정상 동작한다")
    @Test
    void completeProduction_thenPickupReady_fullFlow() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("제작→픽업 전체 흐름 상품", 250000L).order();

        // 승인 → IN_PRODUCTION
        orderApprovalService.approve(order.getId());
        Order afterApprove = orderRepository.findById(order.getId()).orElseThrow();

        // 제작 완료 → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), 1L);
        Order afterCompleteProduction = orderRepository.findById(order.getId()).orElseThrow();

        // 픽업 준비 → PICKUP_READY (기존 흐름과 연결 확인)
        mockMvc.perform(post("/admin/orders/{id}/prepare-pickup", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupDeadlineAt\":\"2026-04-01T18:00:00\"}"))
                .andExpect(status().isOk());

        Order final_ = orderRepository.findById(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(afterApprove.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(afterCompleteProduction.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(final_.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
        });
    }

    // -----------------------------------------------------------------------
    // 배송 흐름: APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING → SHIPPED → DELIVERED
    // -----------------------------------------------------------------------

    @DisplayName("제작 완료 후 배송 흐름 전체가 정상 전이된다")
    @Test
    void shippingFlow_fullTransition() {
        Order order = orderHelper.createMadeToOrderPaidOrder("배송 흐름 상품", 200000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.completeProduction(order.getId(), 1L);

        // APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING
        orderShippingService.prepareShipping(order.getId(), 1L);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.SHIPPING_PREPARING);

        // SHIPPING_PREPARING → SHIPPED
        orderShippingService.markShipped(order.getId(), 1L);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.SHIPPED);

        // SHIPPED → DELIVERED
        orderShippingService.markDelivered(order.getId(), 1L);
        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.DELIVERED);

        // 이력에 배송 전이가 모두 기록됨
        var decisions = orderApprovalHistoryRepository.findByOrderIdOrderByDecidedAtAsc(order.getId()).stream()
                .map(h -> h.getDecision())
                .toList();
        assertThat(decisions).containsExactly(
                OrderApprovalDecision.APPROVE,
                OrderApprovalDecision.PRODUCTION_COMPLETE,
                OrderApprovalDecision.PREPARE_SHIPPING,
                OrderApprovalDecision.SHIP,
                OrderApprovalDecision.DELIVER);
    }

    @DisplayName("배송 전이 HTTP 엔드포인트가 정상 동작한다")
    @Test
    void shippingFlow_httpEndpoints() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("HTTP 배송 상품", 200000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.completeProduction(order.getId(), 1L);

        // prepare-shipping
        mockMvc.perform(post("/admin/orders/{id}/prepare-shipping", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPING_PREPARING"));

        // mark-shipped
        mockMvc.perform(post("/admin/orders/{id}/mark-shipped", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        // mark-delivered
        mockMvc.perform(post("/admin/orders/{id}/mark-delivered", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @DisplayName("주문 이력 조회 HTTP 엔드포인트가 정상 동작한다")
    @Test
    void orderHistory_httpEndpoint() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("이력 조회 상품", 200000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.completeProduction(order.getId(), 1L);

        String body = mockMvc.perform(get("/admin/orders/{id}/history", order.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(body).contains("APPROVE");
            softly.assertThat(body).contains("PRODUCTION_COMPLETE");
        });
    }

    // -----------------------------------------------------------------------
    // expectedShipDate write guard
    // -----------------------------------------------------------------------

    @DisplayName("APPROVED_FULFILLMENT_PENDING 상태에서 출고일 설정 시 400을 반환한다")
    @Test
    void setExpectedShipDate_afterProductionComplete_throwsInvalidInput() {
        Order order = orderHelper.createMadeToOrderPaidOrder("출고일 가드 상품", 150000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.completeProduction(order.getId(), 1L);

        assertThatThrownBy(() ->
                orderProductionService.setExpectedShipDate(order.getId(), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("출고일");
    }

    @DisplayName("SHIPPING_PREPARING 상태에서는 출고일 설정이 가능하다")
    @Test
    void setExpectedShipDate_inShippingPreparing_succeeds() {
        Order order = orderHelper.createMadeToOrderPaidOrder("배송준비 출고일 상품", 150000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.completeProduction(order.getId(), 1L);
        orderShippingService.prepareShipping(order.getId(), 1L);

        LocalDate shipDate = LocalDate.of(2026, 5, 1);
        orderProductionService.setExpectedShipDate(order.getId(), shipDate);

        Fulfillment fulfillment = fulfillmentRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getExpectedShipDate()).isEqualTo(shipDate);
    }
}
