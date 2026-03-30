package com.personal.happygallery.app.order;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.notification.NotificationService;
import com.personal.happygallery.app.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.app.order.port.in.OrderAutoRefundBatchUseCase;
import com.personal.happygallery.app.order.port.out.OrderItemPort;
import com.personal.happygallery.app.order.port.out.OrderStorePort;
import com.personal.happygallery.app.payment.port.out.RefundPort;
import com.personal.happygallery.app.product.port.out.InventoryReaderPort;
import com.personal.happygallery.app.product.port.out.InventoryStorePort;
import com.personal.happygallery.app.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.error.AlreadyRefundedException;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.support.OrderTestHelper;
import com.personal.happygallery.support.OrderStateProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.util.concurrent.CompletableFuture;
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
import static org.mockito.Mockito.doAnswer;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [UseCaseIT] §8.2 주문 승인 모델 검증.
 *
 * <p>Proof (docs/PRD/0001_기준_스펙/spec.md §8.2): 24h 경과 케이스에서 자동환불되고 승인 시도는 409.
 */
@UseCaseIT
class OrderApprovalUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired ProductStorePort productStorePort;
    @Autowired InventoryStorePort inventoryStorePort;
    @Autowired InventoryReaderPort inventoryReaderPort;
    @Autowired OrderStorePort orderStorePort;
    @Autowired OrderItemPort orderItemPort;
    @Autowired OrderStateProbe orderStateProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired RefundPort refundPort;
    @Autowired OrderApprovalUseCase orderApprovalService;
    @Autowired OrderAutoRefundBatchUseCase orderAutoRefundBatchService;
    @Autowired OrderService orderService;
    @Autowired Clock clock;
    @MockitoBean NotificationService notificationService;
    OrderTestHelper orderHelper;

    @BeforeEach
    void setUp() {
        cleanup();
        orderHelper = new OrderTestHelper(
                productStorePort, inventoryStorePort, inventoryReaderPort, orderStorePort, orderItemPort, orderService, clock);
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearOrderData();
    }

    // -----------------------------------------------------------------------
    // Proof: 승인 → APPROVED_FULFILLMENT_PENDING
    // -----------------------------------------------------------------------

    @DisplayName("주문 승인 시 APPROVED_FULFILLMENT_PENDING 상태로 전이된다")
    @Test
    void approve_transitionsToApprovedFulfillmentPending() throws Exception {
        Order order = orderHelper.createReadyStockPaidOrder("테스트 상품", 50000L).order();

        mockMvc.perform(post("/admin/orders/{id}/approve", order.getId()))
                .andExpect(status().isOk());

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE);
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 거절 → REJECTED + 재고 복구 + 환불 기록
    // -----------------------------------------------------------------------

    @DisplayName("주문 거절 시 환불 처리와 재고 복구가 수행된다")
    @Test
    void reject_refundsAndRestoresInventory() throws Exception {
        OrderTestHelper.OrderFixture fixture = orderHelper.createReadyStockPaidOrder("거절 테스트 상품", 30000L);
        Order order = fixture.order();

        // 재고 차감 확인
        assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity()).isEqualTo(0);

        mockMvc.perform(post("/admin/orders/{id}/reject", order.getId()))
                .andExpect(status().isOk());

        Order updated = orderStateProbe.getOrder(order.getId());
        var refunds = orderStateProbe.refunds();
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.REJECTED);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.REJECT);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity()).isEqualTo(1);
            softly.assertThat(refunds).hasSize(1);
            softly.assertThat(refunds.get(0).getOrderId()).isEqualTo(order.getId());
        });
    }

    @DisplayName("주문을 두 번 승인해도 상태 전이와 이력은 한 번만 기록된다")
    @Test
    void approve_twice_keepsSingleTransitionAndHistory() {
        Order order = orderHelper.createReadyStockPaidOrder("중복 승인 테스트 상품", 50000L).order();

        orderApprovalService.approve(order.getId());

        assertThatThrownBy(() -> orderApprovalService.approve(order.getId()))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("승인 대기 상태의 주문만 처리할 수 있습니다.");

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE);
        });
    }

    @DisplayName("주문을 두 번 거절해도 상태 전이와 이력은 한 번만 기록된다")
    @Test
    void reject_twice_keepsSingleTransitionAndHistory() {
        Order order = orderHelper.createReadyStockPaidOrder("중복 거절 테스트 상품", 30000L).order();

        orderApprovalService.reject(order.getId());

        assertThatThrownBy(() -> orderApprovalService.reject(order.getId()))
                .isInstanceOf(AlreadyRefundedException.class);

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.REJECTED);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.REJECT);
            softly.assertThat(orderStateProbe.refunds()).hasSize(1);
        });
    }

    @DisplayName("승인 후 거절을 시도하면 400을 반환하고 환불되지 않는다")
    @Test
    void reject_afterApprove_returns400AndDoesNotRefund() {
        OrderTestHelper.OrderFixture fixture = orderHelper.createReadyStockPaidOrder("승인 후 거절 테스트 상품", 40000L);
        Order order = fixture.order();

        orderApprovalService.approve(order.getId());

        assertThatThrownBy(() -> orderApprovalService.reject(order.getId()))
                .isInstanceOf(HappyGalleryException.class)
                .hasMessageContaining("승인 대기 상태의 주문만 처리할 수 있습니다.");

        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.APPROVED_FULFILLMENT_PENDING);
            softly.assertThat(orderStateProbe.orderApprovalHistory(order.getId()))
                    .extracting("decision")
                    .containsExactly(OrderApprovalDecision.APPROVE);
            softly.assertThat(orderStateProbe.refunds()).isEmpty();
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity()).isEqualTo(0);
        });
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.2): 24h 경과 → 자동환불 → AUTO_REFUND_TIMEOUT + 재고 복구
    // -----------------------------------------------------------------------

    @DisplayName("24시간 초과 주문 자동환불 시 상태 전이와 재고 복구가 수행된다")
    @Test
    void autoRefund_expiredOrder_transitionsAndRestoresInventory() {
        OrderTestHelper.OrderFixture fixture = orderHelper.createExpiredReadyStockPendingOrder("자동환불 상품", 40000L);
        Order order = fixture.order();

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();
        Order updated = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(updated.getStatus()).isEqualTo(OrderStatus.AUTO_REFUND_TIMEOUT);
            softly.assertThat(orderStateProbe.getInventoryByProductId(fixture.product().getId()).getQuantity()).isEqualTo(1);
            softly.assertThat(orderStateProbe.refunds()).hasSize(1);
        });
    }

    // -----------------------------------------------------------------------
    // Proof (DoD §8.2): 자동환불 이후 승인 시도 → 409
    // -----------------------------------------------------------------------

    @DisplayName("자동환불된 주문을 승인하면 409 예외가 발생한다")
    @Test
    void approve_afterAutoRefund_throws409() throws Exception {
        Order order = orderHelper.createExpiredReadyStockPendingOrder("409 테스트 상품", 60000L).order();

        // 배치 실행 → AUTO_REFUND_TIMEOUT
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
        Order order = orderHelper.createExpiredReadyStockPendingOrder("HTTP 409 상품", 70000L).order();

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
        Order order = orderHelper.createExpiredMadeToOrderPendingOrder("제작중 자동환불 제외 상품", 80000L).order();

        // MADE_TO_ORDER 승인 → IN_PRODUCTION
        orderApprovalService.approve(order.getId());

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();

        Order unchanged = orderStateProbe.getOrder(order.getId());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(unchanged.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
            softly.assertThat(orderStateProbe.refunds()).isEmpty();
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 자동환불 이후 거절 시도 HTTP 요청 → 409 응답
    // -----------------------------------------------------------------------

    @DisplayName("자동환불된 주문을 거절하면 409를 반환한다")
    @Test
    void reject_afterAutoRefund_returns409() throws Exception {
        Order order = orderHelper.createExpiredReadyStockPendingOrder("자동환불 후 거절 409 상품", 72000L).order();

        orderAutoRefundBatchService.autoRefundExpired();

        mockMvc.perform(post("/admin/orders/{id}/reject", order.getId()))
                .andExpect(status().isConflict());
    }

    @DisplayName("자동환불 알림이 실패해도 환불 처리는 롤백되지 않는다")
    @Test
    void autoRefund_notificationFailure_doesNotRollbackRefund() {
        Order order1 = orderHelper.createExpiredReadyStockPendingOrder("알림실패 상품1", 45000L).order();
        Order order2 = orderHelper.createExpiredReadyStockPendingOrder("알림실패 상품2", 55000L).order();

        // 첫 번째 주문의 알림만 비동기 실패(호출 스레드에는 전파되지 않음)
        doAnswer(invocation -> {
            CompletableFuture.runAsync(() -> {
                throw new RuntimeException("알림 전송 실패");
            });
            return null;
        })
                .doNothing()
                .when(notificationService)
                .notifyByGuestId(any(), any());

        BatchResult result = orderAutoRefundBatchService.autoRefundExpired();

        Order updated1 = orderStateProbe.getOrder(order1.getId());
        Order updated2 = orderStateProbe.getOrder(order2.getId());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(2);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(updated1.getStatus()).isEqualTo(OrderStatus.AUTO_REFUND_TIMEOUT);
            softly.assertThat(updated2.getStatus()).isEqualTo(OrderStatus.AUTO_REFUND_TIMEOUT);
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 주문 환불 FAILED 건이 admin API에서 orderId와 함께 조회된다
    // -----------------------------------------------------------------------

    @DisplayName("주문 환불 실패 건이 admin 환불 실패 목록에서 orderId와 함께 조회된다")
    @Test
    void listFailedRefunds_orderRefund_returnsOrderIdWithoutNpe() throws Exception {
        Order order = orderHelper.createReadyStockPaidOrder("주문환불실패 상품", 90000L).order();

        // 주문 환불 FAILED 직접 생성 (booking 없는 refund)
        var refund = Refund.forOrder(order.getId(), 90000L);
        refund.markFailed("PG 점검중");
        refundPort.save(refund);

        mockMvc.perform(get("/admin/refunds/failed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].refundId").value(refund.getId()))
                .andExpect(jsonPath("$[0].bookingId", nullValue()))
                .andExpect(jsonPath("$[0].orderId").value(order.getId()))
                .andExpect(jsonPath("$[0].amount").value(90000))
                .andExpect(jsonPath("$[0].failReason").value("PG 점검중"));
    }
}
