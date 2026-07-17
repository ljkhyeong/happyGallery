package com.personal.happygallery.application.order;

import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [UseCaseIT] §8.3 예약 제작 주문 검증.
 *
 * <p>Proof (docs/PRD/0001_기준_스펙/spec.md §8.3): 제작 시작 상태에서 취소 요청 시 "환불 불가"로 처리됨.
 */
@UseCaseIT
class OrderProductionUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired InventoryReaderPort inventoryReaderPort;
    @Autowired OrderStorePort orderStorePort;
    @Autowired OrderItemPort orderItemPort;
    @Autowired UserStorePort userStorePort;
    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired OrderApprovalUseCase orderApprovalService;
    @Autowired OrderProductionUseCase orderProductionService;
    @Autowired OrderPickupUseCase orderPickupService;
    @Autowired OrderShippingUseCase orderShippingService;
    @Autowired OrderService orderService;
    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                AdminPrincipal.apiKey(), null, "ROLE_ADMIN"));
        orderHelper = new OrderTestHelper(
                productStorePort, inventoryStorePort, inventoryReaderPort, orderStorePort, orderItemPort,
                userStorePort, orderService);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        SecurityContextHolder.clearContext();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // MADE_TO_ORDER 승인 → IN_PRODUCTION + Fulfillment 생성
    // -----------------------------------------------------------------------

    @DisplayName("주문제작 상품 주문 승인 시 IN_PRODUCTION으로 전이되고 Fulfillment가 생성된다")
    @Test
    void approve_madeToOrder_transitionsToInProductionAndCreatesFulfillment() {
        Order order = orderHelper.createMadeToOrderPaidOrder("예약 제작 상품", 200000L).order();

        orderApprovalService.approve(order.getId());

        Order updated = orderStateProbe.getOrder(order.getId());
        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
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

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.findFulfillmentByOrderId(order.getId())).isEmpty();
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

        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
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

        Order updated = orderStateProbe.getOrder(order.getId());
        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELAY_REQUESTED);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.DELAY);
        });
    }

    @DisplayName("고객이 배송 지연을 거절하면 주문이 DELAY_REJECTED_CANCELED로 전이되고 환불과 재고 복구가 수행된다")
    @Test
    void cancelForDelayRejection_refundsAndRestoresInventory() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createMadeToOrderPaidOrder("지연 거절 취소 상품", 180000L);
        Order order = fixture.order();
        orderApprovalService.approve(order.getId());

        orderProductionService.cancelForDelayRejection(order.getId(), 1L);

        Order updated = orderStateProbe.getOrder(order.getId());
        var histories = orderStateProbe.orderApprovalHistory(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELAY_REJECTED_CANCELED);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity())
                    .isEqualTo(1);
            softly.assertThat(orderStateProbe.refunds()).hasSize(1);
            softly.assertThat(orderStateProbe.refunds().get(0).getOrderId()).isEqualTo(order.getId());
            softly.assertThat(histories)
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.DELAY_CANCEL);
            softly.assertThat(histories.get(1).getDecidedByAdminId()).isEqualTo(1L);
        });
    }

    @DisplayName("지연 요청 상태에서는 지연 거절 취소를 할 수 없다")
    @Test
    void cancelForDelayRejection_afterDelayAccepted_throwsInvalidInput() {
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 수락 후 취소 불가 상품", 180000L).order();
        orderApprovalService.approve(order.getId());
        orderProductionService.requestDelay(order.getId());

        assertThatThrownBy(() -> orderProductionService.cancelForDelayRejection(order.getId(), 1L))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("지연 거절 취소");

        assertSoftly(softly -> {
            softly.assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                    .isEqualTo(OrderStatus.DELAY_REQUESTED);
            softly.assertThat(orderStateProbe.refunds()).isEmpty();
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
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> orderApprovalService.reject(order.getId()))
                    .isInstanceOf(ProductionRefundNotAllowedException.class);

            // 상태 변경 없음 확인
            Order unchanged = orderStateProbe.getOrder(order.getId());
            softly.assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        });
    }

    // -----------------------------------------------------------------------
    // 제작 완료 → APPROVED_FULFILLMENT_PENDING → 픽업 처리 전체 흐름
    // -----------------------------------------------------------------------

    @DisplayName("제작 완료 처리 시 APPROVED_FULFILLMENT_PENDING 상태로 전이된다")
    @Test
    void completeProduction_transitionsToApprovedFulfillmentPending() {
        Order order = orderHelper.createMadeToOrderPaidOrder("제작완료 상품", 200000L).order();
        orderApprovalService.approve(order.getId());

        // IN_PRODUCTION → completeProduction → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), 1L);

        Order updated = orderStateProbe.getOrder(order.getId());
        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        var histories = orderStateProbe.orderApprovalHistory(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
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

        Order updated = orderStateProbe.getOrder(order.getId());
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

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.RESUME_PRODUCTION);
        });
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
                LocalDateTime.of(2026, 4, 1, 18, 0), 1L);

        var fulfillments = orderStateProbe.fulfillments().stream()
                .filter(f -> f.getOrderId().equals(order.getId()))
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(fulfillments).hasSize(1);
            softly.assertThat(fulfillments.get(0).getType())
                    .isEqualTo(FulfillmentType.PICKUP);
            softly.assertThat(fulfillments.get(0).getPickupDeadlineAt()).isNotNull();
        });
    }

    @DisplayName("제작 완료 후 픽업 완료까지 상태와 이력이 함께 전이된다")
    @Test
    void completeProduction_thenPickupComplete_recordsFullFlow() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("제작→픽업 전체 흐름 상품", 250000L).order();

        // 승인 → IN_PRODUCTION
        orderApprovalService.approve(order.getId());
        Order afterApprove = orderStateProbe.getOrder(order.getId());

        // 제작 완료 → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), 1L);
        Order afterCompleteProduction = orderStateProbe.getOrder(order.getId());

        // 픽업 준비 → PICKUP_READY (기존 흐름과 연결 확인)
        mockMvc.perform(post("/api/v1/admin/orders/{id}/prepare-pickup", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupDeadlineAt\":\"2026-04-01T18:00:00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/{id}/complete-pickup", order.getId()))
                .andExpect(status().isOk());

        Order final_ = orderStateProbe.getOrder(order.getId());
        var decisions = orderStateProbe.orderApprovalHistoryOrdered(order.getId()).stream()
                .map(history -> history.getDecision())
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(afterApprove.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(afterCompleteProduction.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(final_.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
            softly.assertThat(decisions).containsExactly(
                    OrderApprovalDecision.APPROVE,
                    OrderApprovalDecision.PRODUCTION_COMPLETE,
                    OrderApprovalDecision.PICKUP_READY,
                    OrderApprovalDecision.PICKUP_COMPLETE);
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
        assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                .isEqualTo(OrderStatus.SHIPPING_PREPARING);

        // SHIPPING_PREPARING → SHIPPED
        orderShippingService.markShipped(order.getId(), 1L);
        assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                .isEqualTo(OrderStatus.SHIPPED);

        // SHIPPED → DELIVERED
        orderShippingService.markDelivered(order.getId(), 1L);
        assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                .isEqualTo(OrderStatus.DELIVERED);

        // 이력에 배송 전이가 모두 기록됨
        var decisions = orderStateProbe.orderApprovalHistoryOrdered(order.getId()).stream()
                .map(h -> h.getDecision())
                .toList();
        assertThat(decisions).containsExactly(
                OrderApprovalDecision.APPROVE,
                OrderApprovalDecision.PRODUCTION_COMPLETE,
                OrderApprovalDecision.PREPARE_SHIPPING,
                OrderApprovalDecision.SHIP,
                OrderApprovalDecision.DELIVER);
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

        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getExpectedShipDate()).isEqualTo(shipDate);
    }
}
