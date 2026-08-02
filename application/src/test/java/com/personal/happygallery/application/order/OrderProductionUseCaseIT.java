package com.personal.happygallery.application.order;

import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase.ProposeDelayCommand;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase.SetExpectedShipDateCommand;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.InventoryReaderPort;
import com.personal.happygallery.application.product.port.out.InventoryStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.ProductionRefundNotAllowedException;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.MadeToOrderConsent;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderDelayDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final long ADMIN_ID = 1L;

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
    @Autowired OrderCustomerActionUseCase orderCustomerActionUseCase;
    @Autowired OrderPickupUseCase orderPickupService;
    @Autowired OrderShippingUseCase orderShippingService;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired OrderService orderService;
    @Autowired JdbcTemplate jdbcTemplate;
    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
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
    // MADE_TO_ORDER 승인 → IN_PRODUCTION + 결제 시점 Fulfillment 유지
    // -----------------------------------------------------------------------

    @DisplayName("주문제작 상품 승인 시 결제에서 정한 배송 방식이 유지된다")
    @Test
    void approve_madeToOrder_transitionsToInProductionAndCreatesFulfillment() {
        Order order = orderHelper.createMadeToOrderPaidShippingOrder("예약 제작 상품", 200000L).order();

        orderApprovalService.approve(order.getId(), ADMIN_ID);

        Order updated = orderStateProbe.getOrder(order.getId());
        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(fulfillment.getType()).isEqualTo(FulfillmentType.SHIPPING);
        });
    }

    // -----------------------------------------------------------------------
    // READY_STOCK 승인 → 기존 상태 흐름 + 결제 시점 Fulfillment 유지
    // -----------------------------------------------------------------------

    @DisplayName("기성품 주문 승인 시 APPROVED_FULFILLMENT_PENDING 상태를 유지한다")
    @Test
    void approve_readyStock_remainsApprovedFulfillmentPending() {
        Order order = orderHelper.createReadyStockPaidOrder("기성품", 50000L).order();

        orderApprovalService.approve(order.getId(), ADMIN_ID);

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.findFulfillmentByOrderId(order.getId()))
                    .hasValueSatisfying(fulfillment ->
                            softly.assertThat(fulfillment.getType()).isEqualTo(FulfillmentType.PICKUP));
        });
    }

    // -----------------------------------------------------------------------
    // 예상 출고일 설정
    // -----------------------------------------------------------------------

    @DisplayName("예상 출고일 변경 시 관리자와 이전·이후 날짜가 이력에 기록된다")
    @Test
    void setExpectedShipDate_updatesShipDateOnFulfillment() {
        Order order = orderHelper.createMadeToOrderPaidShippingOrder("출고일 설정 상품", 150000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);

        LocalDate firstShipDate = LocalDate.of(2026, 4, 15);
        LocalDate changedShipDate = LocalDate.of(2026, 4, 20);
        orderProductionService.setExpectedShipDate(
                new SetExpectedShipDateCommand(order.getId(), firstShipDate, ADMIN_ID));
        orderProductionService.setExpectedShipDate(
                new SetExpectedShipDateCommand(order.getId(), changedShipDate, ADMIN_ID));

        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        var histories = orderStateProbe.orderApprovalHistory(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(fulfillment.getExpectedShipDate()).isEqualTo(changedShipDate);
            softly.assertThat(histories)
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.SHIP_DATE_UPDATED,
                            OrderApprovalDecision.SHIP_DATE_UPDATED);
            softly.assertThat(histories.get(1).getDecidedByAdminId()).isEqualTo(ADMIN_ID);
            softly.assertThat(histories.get(1).getReason())
                    .isEqualTo("예상 출고일: 미설정 -> 2026-04-15");
            softly.assertThat(histories.get(2).getDecidedByAdminId()).isEqualTo(ADMIN_ID);
            softly.assertThat(histories.get(2).getReason())
                    .isEqualTo("예상 출고일: 2026-04-15 -> 2026-04-20");
        });
    }

    // -----------------------------------------------------------------------
    // DELAY_ACCEPTED 전환 (고객 동의)
    // -----------------------------------------------------------------------

    @DisplayName("관리자가 제작 지연을 제안하면 고객 응답 대기 상태와 알림이 기록된다")
    @Test
    void proposeDelay_transitionsToConsentPending() {
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 상품", 180000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);

        orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID));

        Order updated = orderStateProbe.getOrder(order.getId());
        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELAY_CONSENT_PENDING);
            var histories = orderStateProbe.orderApprovalHistory(order.getId());
            softly.assertThat(histories)
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE, OrderApprovalDecision.DELAY);
            softly.assertThat(histories.get(1).getDecidedByAdminId()).isEqualTo(ADMIN_ID);
            softly.assertThat(notificationOutboxRepository.findAll())
                    .extracting("eventType")
                    .contains(NotificationEventType.ORDER_DELAY_REQUESTED);
        });
    }

    @DisplayName("현재 상품 유형이 바뀌어도 미승인 주문제작 스냅샷에는 지연을 제안할 수 없다")
    @Test
    void proposeDelay_unapprovedMadeToOrderSnapshot_rejectsWithoutMutation() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createMadeToOrderPaidOrder("미승인 주문제작 상품", 180_000L);
        Order order = fixture.order();
        long outboxCountBefore = notificationOutboxRepository.count();

        jdbcTemplate.update("""
                UPDATE products
                SET type = 'READY_STOCK', production_lead_days = NULL, version = version + 1
                WHERE id = ?
                """, fixture.product().getId());

        assertThatThrownBy(() ->
                orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("제작 중");

        assertSoftly(softly -> {
            softly.assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                    .isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId())).isEmpty();
            softly.assertThat(notificationOutboxRepository.count()).isEqualTo(outboxCountBefore);
        });
    }

    @DisplayName("미승인 혼합 주문에는 지연을 제안할 수 없고 이력과 알림도 남지 않는다")
    @Test
    void proposeDelay_unapprovedMixedOrder_rejectsWithoutMutation() {
        Product readyStock = orderHelper.createReadyStockProduct("혼합 기성품", 50_000L, 1);
        Product madeToOrder = productStorePort.save(new Product(
                "혼합 주문제작 상품",
                ProductType.MADE_TO_ORDER,
                null,
                120_000L,
                null,
                null,
                "재료: 테스트 재료\n크기: 테스트 규격\n사양: 고정 사양",
                "직사광선을 피해 보관하세요.",
                14));
        inventoryStorePort.save(new Inventory(madeToOrder, 1));
        Order order = orderService.createMemberOrder(
                orderHelper.createMemberOwner().getId(),
                List.of(orderItemRequest(readyStock), orderItemRequest(madeToOrder)),
                FulfillmentType.PICKUP,
                null,
                0L,
                MadeToOrderConsent.current(LocalDateTime.of(2026, 1, 1, 0, 0)));
        long outboxCountBefore = notificationOutboxRepository.count();

        assertThatThrownBy(() ->
                orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("제작 중");

        assertSoftly(softly -> {
            softly.assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                    .isEqualTo(OrderStatus.PAID_APPROVAL_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId())).isEmpty();
            softly.assertThat(notificationOutboxRepository.count()).isEqualTo(outboxCountBefore);
        });
    }

    @DisplayName("고객이 배송 지연을 거절하면 주문이 DELAY_REJECTED_CANCELED로 전이되고 환불과 재고 복구가 수행된다")
    @Test
    void cancelForDelayRejection_refundsAndRestoresInventory() {
        OrderTestHelper.OrderFixture fixture =
                orderHelper.createMadeToOrderPaidOrder("지연 거절 취소 상품", 180000L);
        Order order = fixture.order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID));

        var result = orderProductionService.cancelForDelayRejection(order.getId(), 1L);

        Order updated = orderStateProbe.getOrder(order.getId());
        var histories = orderStateProbe.orderApprovalHistory(order.getId());
        var refunds = orderStateProbe.refunds();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELAY_REJECTED_CANCELED);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity())
                    .isEqualTo(1);
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refunds.getFirst().getOrderId()).isEqualTo(order.getId());
            softly.assertThat(result.refund().getId()).isNotNull();
            softly.assertThat(result.refund().getStatus()).isEqualTo(RefundStatus.REQUESTED);
            softly.assertThat(histories)
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.DELAY_CANCEL);
            softly.assertThat(histories.get(2).getDecidedByAdminId()).isEqualTo(1L);
        });
    }

    @DisplayName("지연 수락 상태에서는 지연 거절 취소를 할 수 없다")
    @Test
    void cancelForDelayRejection_afterDelayAccepted_throwsInvalidInput() {
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 수락 후 취소 불가 상품", 180000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID));
        orderCustomerActionUseCase.respondToMemberDelay(
                order.getId(), order.getUserId(), OrderDelayDecision.ACCEPT);

        assertThatThrownBy(() -> orderProductionService.cancelForDelayRejection(order.getId(), 1L))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("지연 거절 취소");

        assertSoftly(softly -> {
            softly.assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                    .isEqualTo(OrderStatus.DELAY_ACCEPTED);
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
        orderApprovalService.approve(order.getId(), ADMIN_ID);

        // IN_PRODUCTION 상태에서 reject → ProductionRefundNotAllowedException
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> orderApprovalService.reject(order.getId(), ADMIN_ID))
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
        orderApprovalService.approve(order.getId(), ADMIN_ID);

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

    @DisplayName("지연 수락 상태에서도 제작 완료 처리가 가능하다")
    @Test
    void completeProduction_fromDelayRequested_alsoWorks() {
        Order order = orderHelper.createMadeToOrderPaidOrder("지연 후 제작완료 상품", 180000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID));
        orderCustomerActionUseCase.respondToMemberDelay(
                order.getId(), order.getUserId(), OrderDelayDecision.ACCEPT);

        // DELAY_ACCEPTED → completeProduction → APPROVED_FULFILLMENT_PENDING
        orderProductionService.completeProduction(order.getId(), null);

        Order updated = orderStateProbe.getOrder(order.getId());
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
    }

    // -----------------------------------------------------------------------
    // DELAY_ACCEPTED → resumeAfterDelay → 상품 유형별 처리 상태
    // -----------------------------------------------------------------------

    @DisplayName("주문제작 지연을 수락한 뒤 처리를 재개하면 제작 중으로 돌아간다")
    @Test
    void resumeAfterDelay_forMadeToOrder_returnsToProduction() {
        Order order = orderHelper.createMadeToOrderPaidOrder("재개 상품", 180000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID));
        orderCustomerActionUseCase.respondToMemberDelay(
                order.getId(), order.getUserId(), OrderDelayDecision.ACCEPT);

        orderProductionService.resumeAfterDelay(order.getId(), 1L);

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.APPROVE,
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.DELAY_ACCEPT,
                            OrderApprovalDecision.RESUME_PRODUCTION);
        });
    }

    @DisplayName("기성품 품절 지연을 수락한 뒤 처리를 재개하면 승인된 이행 대기로 전이된다")
    @Test
    void resumeAfterDelay_forReadyStock_movesToFulfillmentPending() {
        Order order = orderHelper.createReadyStockPaidOrder("재입고 상품", 80_000L).order();
        orderProductionService.proposeDelay(new ProposeDelayCommand(order.getId(), ADMIN_ID));
        orderCustomerActionUseCase.respondToMemberDelay(
                order.getId(), order.getUserId(), OrderDelayDecision.ACCEPT);

        orderProductionService.resumeAfterDelay(order.getId(), ADMIN_ID);

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus())
                    .isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(
                            OrderApprovalDecision.DELAY,
                            OrderApprovalDecision.DELAY_ACCEPT,
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
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.completeProduction(order.getId(), 1L);

        orderPickupService.markPickupReady(order.getId(),
                LocalDateTime.of(2026, 4, 1, 18, 0), 1L);

        var fulfillments = orderStateProbe.fulfillments().stream()
                .filter(f -> f.getOrderId().equals(order.getId()))
                .toList();
        var fulfillment = fulfillments.getFirst();
        assertSoftly(softly -> {
            softly.assertThat(fulfillments).hasSize(1);
            softly.assertThat(fulfillment.getType())
                    .isEqualTo(FulfillmentType.PICKUP);
            softly.assertThat(fulfillment.getPickupDeadlineAt()).isNotNull();
            softly.assertThat(notificationOutboxRepository.findAll())
                    .extracting("eventType")
                    .contains(NotificationEventType.ORDER_PICKUP_READY);
        });
    }

    @DisplayName("제작 완료 후 픽업 완료까지 상태와 이력이 함께 전이된다")
    @Test
    void completeProduction_thenPickupComplete_recordsFullFlow() throws Exception {
        Order order = orderHelper.createMadeToOrderPaidOrder("제작→픽업 전체 흐름 상품", 250000L).order();

        // 승인 → IN_PRODUCTION
        orderApprovalService.approve(order.getId(), ADMIN_ID);
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
        Order order = orderHelper.createMadeToOrderPaidShippingOrder("배송 흐름 상품", 200000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.completeProduction(order.getId(), 1L);

        // APPROVED_FULFILLMENT_PENDING → SHIPPING_PREPARING
        orderShippingService.prepareShipping(order.getId(), 1L);
        assertThat(orderStateProbe.getOrder(order.getId()).getStatus())
                .isEqualTo(OrderStatus.SHIPPING_PREPARING);

        // SHIPPING_PREPARING → SHIPPED
        orderShippingService.markShipped(order.getId(), "CJ대한통운", "1234567890", 1L);
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
        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(decisions).containsExactly(
                    OrderApprovalDecision.APPROVE,
                    OrderApprovalDecision.PRODUCTION_COMPLETE,
                    OrderApprovalDecision.PREPARE_SHIPPING,
                    OrderApprovalDecision.SHIP,
                    OrderApprovalDecision.DELIVER);
            softly.assertThat(fulfillment.getCarrier()).isEqualTo("CJ대한통운");
            softly.assertThat(fulfillment.getTrackingNumber()).isEqualTo("1234567890");
            softly.assertThat(notificationOutboxRepository.findAll())
                    .extracting("eventType")
                    .contains(NotificationEventType.ORDER_SHIPPED);
        });
    }

    @DisplayName("관리자는 고객이 선택한 수령 방법과 다른 이행 흐름을 시작할 수 없다")
    @Test
    void fulfillmentTransition_mustMatchCustomerSelection() {
        Order pickupOrder = orderHelper.createReadyStockPaidOrder("픽업 선택 상품", 60_000L).order();
        Order shippingOrder = orderHelper.createMadeToOrderPaidShippingOrder("배송 선택 상품", 160_000L).order();
        orderApprovalService.approve(pickupOrder.getId(), ADMIN_ID);
        orderApprovalService.approve(shippingOrder.getId(), ADMIN_ID);
        orderProductionService.completeProduction(shippingOrder.getId(), ADMIN_ID);

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> orderShippingService.prepareShipping(pickupOrder.getId(), ADMIN_ID))
                    .isInstanceOf(HappyGalleryException.class)
                    .hasMessageContaining("배송 이행");
            softly.assertThatThrownBy(() -> orderPickupService.markPickupReady(
                            shippingOrder.getId(), LocalDateTime.of(2026, 4, 1, 18, 0), ADMIN_ID))
                    .isInstanceOf(HappyGalleryException.class)
                    .hasMessageContaining("픽업 주문");
            softly.assertThat(orderStateProbe.getOrder(pickupOrder.getId()).getStatus())
                    .isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.getOrder(shippingOrder.getId()).getStatus())
                    .isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
        });
    }

    // -----------------------------------------------------------------------
    // expectedShipDate write guard
    // -----------------------------------------------------------------------

    @DisplayName("APPROVED_FULFILLMENT_PENDING 상태에서 출고일 설정 시 400을 반환한다")
    @Test
    void setExpectedShipDate_afterProductionComplete_throwsInvalidInput() {
        Order order = orderHelper.createMadeToOrderPaidOrder("출고일 가드 상품", 150000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.completeProduction(order.getId(), 1L);

        assertThatThrownBy(() ->
                orderProductionService.setExpectedShipDate(new SetExpectedShipDateCommand(
                        order.getId(), LocalDate.of(2026, 5, 1), ADMIN_ID)))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("출고일");
    }

    @DisplayName("SHIPPING_PREPARING 상태에서는 출고일 설정이 가능하다")
    @Test
    void setExpectedShipDate_inShippingPreparing_succeeds() {
        Order order = orderHelper.createMadeToOrderPaidShippingOrder("배송준비 출고일 상품", 150000L).order();
        orderApprovalService.approve(order.getId(), ADMIN_ID);
        orderProductionService.completeProduction(order.getId(), 1L);
        orderShippingService.prepareShipping(order.getId(), 1L);

        LocalDate shipDate = LocalDate.of(2026, 5, 1);
        orderProductionService.setExpectedShipDate(
                new SetExpectedShipDateCommand(order.getId(), shipDate, ADMIN_ID));

        Fulfillment fulfillment = orderStateProbe.findFulfillmentByOrderId(order.getId()).orElseThrow();
        assertThat(fulfillment.getExpectedShipDate()).isEqualTo(shipDate);
    }

    private static OrderService.OrderItemRequest orderItemRequest(Product product) {
        return new OrderService.OrderItemRequest(
                product.getId(),
                product.getName(),
                product.getType(),
                1,
                product.getPrice(),
                product.getSpecification(),
                product.getCareInstructions(),
                product.getProductionLeadDays());
    }
}
