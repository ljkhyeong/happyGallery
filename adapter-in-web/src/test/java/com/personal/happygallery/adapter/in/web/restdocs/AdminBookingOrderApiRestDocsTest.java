package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminBookingController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderApprovalController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderPickupController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderProductionController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderQueryController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderShippingController;
import com.personal.happygallery.adapter.in.web.admin.AdminSlotSessionController;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateAdminBookingRequest;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingCreateUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.CompletionResult;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.TaskView;
import com.personal.happygallery.application.booking.port.in.BookingNoShowUseCase;
import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderFulfillmentResponse;
import com.personal.happygallery.application.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderHistoryResponse;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingCancellationTaskStatus;
import com.personal.happygallery.domain.booking.BookingCancellationTaskType;
import com.personal.happygallery.domain.booking.BookingSource;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import com.personal.happygallery.domain.order.TrackingRegistrationStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBookingOrderApiRestDocsTest extends RestDocsTestSupport {

    private static final String SNIPPET_GROUP = "admin-api-rest-docs-test";

    private MockMvc mockMvc;
    private AdminBookingQueryUseCase adminBookingQueryUseCase;
    private AdminBookingCreateUseCase adminBookingCreateUseCase;
    private AdminBookingSearchUseCase adminBookingSearchUseCase;
    private BookingNoShowUseCase bookingNoShowUseCase;
    private BookingSettlementUseCase bookingSettlementUseCase;
    private AdminBookingCancelUseCase adminBookingCancelUseCase;
    private BookingCancellationTaskUseCase bookingCancellationTaskUseCase;
    private AdminOrderQueryUseCase adminOrderQueryUseCase;
    private AdminOrderSearchUseCase adminOrderSearchUseCase;
    private OrderApprovalUseCase orderApprovalUseCase;
    private OrderProductionUseCase orderProductionUseCase;
    private OrderPickupUseCase orderPickupUseCase;
    private OrderShippingUseCase orderShippingUseCase;
    private PickupExpireBatchUseCase pickupExpireBatchUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        adminBookingQueryUseCase = mock(AdminBookingQueryUseCase.class);
        adminBookingCreateUseCase = mock(AdminBookingCreateUseCase.class);
        adminBookingSearchUseCase = mock(AdminBookingSearchUseCase.class);
        bookingNoShowUseCase = mock(BookingNoShowUseCase.class);
        bookingSettlementUseCase = mock(BookingSettlementUseCase.class);
        adminBookingCancelUseCase = mock(AdminBookingCancelUseCase.class);
        bookingCancellationTaskUseCase = mock(BookingCancellationTaskUseCase.class);
        adminOrderQueryUseCase = mock(AdminOrderQueryUseCase.class);
        adminOrderSearchUseCase = mock(AdminOrderSearchUseCase.class);
        orderApprovalUseCase = mock(OrderApprovalUseCase.class);
        orderProductionUseCase = mock(OrderProductionUseCase.class);
        orderPickupUseCase = mock(OrderPickupUseCase.class);
        orderShippingUseCase = mock(OrderShippingUseCase.class);
        pickupExpireBatchUseCase = mock(PickupExpireBatchUseCase.class);

        Booking booking = RestDocsFixtures.booking();
        Refund bookingRefund = RestDocsFixtures.bookingRefund();
        Order order = RestDocsFixtures.order();
        Refund orderRefund = RestDocsFixtures.orderRefund();

        when(adminBookingQueryUseCase.listBookings(any(), any()))
                .thenReturn(List.of(adminBookingResponse()));
        when(adminBookingCreateUseCase.create(any())).thenReturn(adminBookingResponse());
        when(adminBookingSearchUseCase.search(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminBookingSearchRow()), 0, 20, 1));
        when(bookingNoShowUseCase.markNoShow(100L, ADMIN_USER_ID)).thenReturn(booking);
        when(bookingSettlementUseCase.markBalancePaid(100L, ADMIN_USER_ID)).thenReturn(booking);
        when(bookingSettlementUseCase.updateArrears(eq(100L), anyBoolean(), eq(ADMIN_USER_ID)))
                .thenReturn(booking);
        when(bookingSettlementUseCase.complete(100L, ADMIN_USER_ID)).thenReturn(booking);
        when(adminBookingCancelUseCase.cancel(any()))
                .thenReturn(new AdminCancelResult(booking, false, bookingRefund, false, false));
        when(adminBookingCancelUseCase.cancelSession(any()))
                .thenReturn(new CancelSessionResult(4, 2, 2, 1, 0));
        TaskView pendingTask =
                bookingCancellationTask(BookingCancellationTaskStatus.PENDING);
        TaskView completedTask =
                bookingCancellationTask(BookingCancellationTaskStatus.COMPLETED);
        when(bookingCancellationTaskUseCase.listPending()).thenReturn(List.of(pendingTask));
        when(bookingCancellationTaskUseCase.complete(501L, ADMIN_USER_ID))
                .thenReturn(new CompletionResult(completedTask, true));
        when(adminOrderQueryUseCase.listOrders(any(), any(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(adminOrderResponse()), "cursor-next", true));
        when(adminOrderQueryUseCase.getFulfillment(200L)).thenReturn(adminOrderFulfillmentResponse());
        when(adminOrderSearchUseCase.search(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminOrderSearchRow()), 0, 20, 1));
        when(orderApprovalUseCase.reject(200L, ADMIN_USER_ID))
                .thenReturn(new OrderApprovalUseCase.RejectResult(order, orderRefund));
        when(orderProductionUseCase.resumeAfterDelay(200L, ADMIN_USER_ID))
                .thenReturn(production(OrderStatus.APPROVED_FULFILLMENT_PENDING));
        when(orderProductionUseCase.completeProduction(200L, ADMIN_USER_ID))
                .thenReturn(production(OrderStatus.APPROVED_FULFILLMENT_PENDING));
        when(orderProductionUseCase.setExpectedShipDate(any()))
                .thenReturn(production(OrderStatus.IN_PRODUCTION));
        when(orderProductionUseCase.proposeDelay(any()))
                .thenReturn(production(OrderStatus.DELAY_CONSENT_PENDING));
        when(orderProductionUseCase.cancelForDelayRejection(200L, ADMIN_USER_ID))
                .thenReturn(new OrderProductionUseCase.DelayCancellationResult(
                        production(OrderStatus.DELAY_REJECTED_CANCELED), orderRefund));
        when(orderPickupUseCase.markPickupReady(eq(200L), any(), eq(ADMIN_USER_ID)))
                .thenReturn(pickup(OrderStatus.PICKUP_READY));
        when(orderPickupUseCase.confirmPickup(200L, ADMIN_USER_ID))
                .thenReturn(pickup(OrderStatus.PICKED_UP));
        when(orderShippingUseCase.prepareShipping(200L, ADMIN_USER_ID))
                .thenReturn(shipping(OrderStatus.SHIPPING_PREPARING));
        when(orderShippingUseCase.markShipped(
                200L,
                ShippingCarrier.CJ_LOGISTICS,
                "CJ대한통운",
                "1234567890",
                ADMIN_USER_ID))
                .thenReturn(shipping(OrderStatus.SHIPPED));
        when(orderShippingUseCase.markDelivered(200L, ADMIN_USER_ID))
                .thenReturn(shipping(OrderStatus.DELIVERED));
        when(adminOrderQueryUseCase.getOrderHistory(200L)).thenReturn(List.of(orderHistory()));
        when(pickupExpireBatchUseCase.expirePickups()).thenReturn(batchResult());

        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
                new AdminSlotSessionController(adminBookingCancelUseCase),
                new AdminBookingController(
                        adminBookingQueryUseCase,
                        adminBookingCreateUseCase,
                        adminBookingSearchUseCase,
                        bookingNoShowUseCase,
                        bookingSettlementUseCase,
                        adminBookingCancelUseCase,
                        bookingCancellationTaskUseCase),
                new AdminOrderQueryController(adminOrderQueryUseCase, adminOrderSearchUseCase),
                new AdminOrderApprovalController(orderApprovalUseCase),
                new AdminOrderProductionController(orderProductionUseCase),
                new AdminOrderPickupController(orderPickupUseCase, pickupExpireBatchUseCase),
                new AdminOrderShippingController(orderShippingUseCase));
    }

    @Test
    @DisplayName("관리자 수기 예약 등록 API를 문서화한다")
    void admin_create_booking() throws Exception {
        CreateAdminBookingRequest request = new CreateAdminBookingRequest(
                42L, "홍길동", "010-1234-5678", 3, BookingSource.PHONE, true);

        mockMvc.perform(post("/api/v1/admin/bookings")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content(JsonMapper.builder().build().writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerSummary.type").value("GUEST"))
                .andExpect(jsonPath("$.customerSummary.name").value("홍길동"))
                .andExpect(jsonPath("$.source").value("PHONE"))
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("관리자 예약 목록 API를 문서화한다")
    void admin_list_bookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("date", "2026-05-07")
                        .param("status", "BOOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerSummary.phone").value("01012345678"))
                .andExpect(jsonPath("$[0].participantCount").value(3));
    }

    @Test
    @DisplayName("관리자 예약 검색 API를 문서화한다")
    void admin_search_bookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/search")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("status", "BOOKED")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31")
                        .param("keyword", "홍길동")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 예약 결석 처리 API를 문서화한다")
    void admin_mark_booking_no_show() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/{bookingId}/no-show", 100L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("관리자 예약 잔금 결제 API를 문서화한다")
    void admin_mark_booking_balance_paid() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/{bookingId}/balance-payment", 100L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participantCount").value(3));
    }

    @Test
    @DisplayName("관리자 예약 미수 설정 API를 문서화한다")
    void admin_update_booking_arrears() throws Exception {
        mockMvc.perform(put("/api/v1/admin/bookings/{bookingId}/arrears", 100L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"arrears\":true}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 예약 완료 API를 문서화한다")
    void admin_complete_booking() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/{bookingId}/complete", 100L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 예약 취소 API를 문서화한다")
    void admin_cancel_booking() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/{bookingId}/cancel", 100L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"공방 사정으로 수업이 취소되었습니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(100L))
                .andExpect(jsonPath("$.participantCount").value(3))
                .andExpect(jsonPath("$.depositRefundAmount").value(15000L))
                .andExpect(jsonPath("$.manualCompensationRequired").value(false));
    }

    @Test
    @DisplayName("관리자 예약 취소 후속 작업 목록 API를 문서화한다")
    void admin_list_booking_cancellation_tasks() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings/cancellation-tasks")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(501L))
                .andExpect(jsonPath("$[0].bookingNumber").value("BK-00000100"))
                .andExpect(jsonPath("$[0].type").value("BALANCE_SETTLEMENT"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("관리자 예약 취소 후속 작업 완료 API를 문서화한다")
    void admin_complete_booking_cancellation_task() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/cancellation-tasks/{taskId}/complete", 501L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.taskId").value(501L))
                .andExpect(jsonPath("$.task.status").value("COMPLETED"))
                .andExpect(jsonPath("$.changed").value(true));
    }

    @Test
    @DisplayName("관리자 수업 회차 취소 API를 문서화한다")
    void admin_cancel_slot_session() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots/{slotId}/cancel-session", 42L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"강사 사정으로 해당 회차가 취소되었습니다.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canceledBookings").value(4))
                .andExpect(jsonPath("$.passCreditsRestored").value(2))
                .andExpect(jsonPath("$.depositRefundsRequested").value(2))
                .andExpect(jsonPath("$.balanceSettlementsRequired").value(1))
                .andExpect(jsonPath("$.manualCompensationsRequired").value(0));
    }

    @Test
    @DisplayName("관리자 주문 목록 API를 문서화한다")
    void admin_list_orders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("status", "PAID_APPROVAL_PENDING")
                        .param("cursor", "cursor")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 이행 상세 API를 문서화한다")
    void admin_get_order_fulfillment() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/{id}/fulfillment", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingAddress.postalCode").value("06236"));
    }

    @Test
    @DisplayName("관리자 주문 검색 API를 문서화한다")
    void admin_search_orders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/search")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("status", "PAID_APPROVAL_PENDING")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31")
                        .param("keyword", "홍길동")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 승인 API를 문서화한다")
    void admin_approve_order() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/approve", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 거절 API를 문서화한다")
    void admin_reject_order() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/reject", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refund.refundId").value(901))
                .andExpect(jsonPath("$.refund.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("관리자 주문 지연 후 처리 재개 API를 문서화한다")
    void admin_resume_order_after_delay() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/resume-after-delay", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 제작 완료 API를 문서화한다")
    void admin_complete_order_production() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/complete-production", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 예상 출고일 변경 API를 문서화한다")
    void admin_set_expected_ship_date() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/expected-ship-date", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"expectedShipDate\":\"2026-05-08\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 배송 지연 요청 API를 문서화한다")
    void admin_request_order_delay() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/delay", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 지연 거절 취소 API를 문서화한다")
    void admin_cancel_order_for_delay_rejection() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/cancel-for-delay-rejection", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refund.refundId").value(901))
                .andExpect(jsonPath("$.refund.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("관리자 주문 픽업 준비 API를 문서화한다")
    void admin_prepare_pickup() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/prepare-pickup", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"pickupDeadlineAt\":\"2026-05-10T21:00:00\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 픽업 완료 API를 문서화한다")
    void admin_complete_pickup() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/complete-pickup", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 배송 준비 API를 문서화한다")
    void admin_prepare_shipping() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/prepare-shipping", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 배송 출발 API를 문서화한다")
    void admin_mark_shipped() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/mark-shipped", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"carrier":"CJ대한통운","carrierCode":"CJ_LOGISTICS",
                                 "trackingNumber":"1234567890"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 배송 완료 API를 문서화한다")
    void admin_mark_delivered() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/mark-delivered", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 이력 API를 문서화한다")
    void admin_get_order_history() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders/{id}/history", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 픽업 만료 배치 API를 문서화한다")
    void admin_expire_pickups() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/expire-pickups")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    private static AdminBookingResponse adminBookingResponse() {
        return new AdminBookingResponse(
                100L,
                "BK-00000100",
                new AdminBookingResponse.CustomerSummary("GUEST", "홍길동", "01012345678"),
                "향수 원데이", LocalDateTime.of(2026, 5, 7, 19, 0),
                LocalDateTime.of(2026, 5, 7, 21, 0), "BOOKED",
                "PHONE",
                3, 15000L, LocalDateTime.of(2026, 5, 1, 20, 50),
                135000L, "UNPAID", null, false, false);
    }

    private static TaskView bookingCancellationTask(
            BookingCancellationTaskStatus status
    ) {
        boolean completed = status == BookingCancellationTaskStatus.COMPLETED;
        return new TaskView(
                501L,
                100L,
                "BK-00000100",
                BookingCancellationTaskType.BALANCE_SETTLEMENT,
                status,
                "향수 원데이",
                LocalDateTime.of(2026, 5, 7, 19, 0),
                135000L,
                0L,
                "공방 사정으로 수업이 취소되었습니다.",
                LocalDateTime.of(2026, 5, 1, 21, 0),
                completed ? ADMIN_USER_ID : null,
                completed ? LocalDateTime.of(2026, 5, 1, 21, 10) : null);
    }

    private static AdminBookingSearchRow adminBookingSearchRow() {
        return new AdminBookingSearchRow(100L, "BK-00000100", "GUEST", "홍길동", "01012345678",
                "향수 원데이", LocalDateTime.of(2026, 5, 7, 19, 0),
                LocalDateTime.of(2026, 5, 7, 21, 0), "BOOKED",
                "PHONE",
                3, 15000L, LocalDateTime.of(2026, 5, 1, 20, 50),
                135000L, "UNPAID", null, false, false,
                LocalDateTime.of(2026, 5, 1, 20, 50).atOffset(ZoneOffset.UTC));
    }

    private static AdminOrderResponse adminOrderResponse() {
        return new AdminOrderResponse(
                200L,
                "ORD-00000200",
                OrderStatus.PAID_APPROVAL_PENDING,
                39000L,
                0L,
                FulfillmentType.PICKUP,
                List.of(new AdminOrderResponse.OrderItemView(
                        1L, "시그니처 캔들", ProductType.READY_STOCK,
                        1, 39000L, null, null, null)),
                LocalDateTime.of(2026, 5, 1, 20, 55),
                LocalDateTime.of(2026, 5, 1, 21, 15),
                LocalDateTime.of(2026, 5, 1, 20, 50).atOffset(ZoneOffset.UTC));
    }

    private static AdminOrderFulfillmentResponse adminOrderFulfillmentResponse() {
        return new AdminOrderFulfillmentResponse(
                200L,
                "SHIPPING",
                new ShippingAddress("홍길동", "01012345678", "06236", "서울시 강남구 테헤란로 1", "2층"),
                LocalDate.of(2026, 5, 8),
                null,
                "CJ대한통운",
                "1234567890",
                ShippingCarrier.CJ_LOGISTICS,
                TrackingRegistrationStatus.ACTIVE,
                ShipmentTrackingStatus.IN_TRANSIT,
                "배송중",
                LocalDateTime.of(2026, 5, 2, 9, 0),
                List.of());
    }

    private static AdminOrderSearchRow adminOrderSearchRow() {
        return new AdminOrderSearchRow(200L, "ORD-00000200", "PAID_APPROVAL_PENDING", 39000L,
                "홍길동", "01012345678",
                LocalDateTime.of(2026, 5, 1, 20, 55),
                LocalDateTime.of(2026, 5, 1, 21, 15),
                LocalDateTime.of(2026, 5, 1, 20, 50).atOffset(ZoneOffset.UTC));
    }

    private static OrderProductionUseCase.ProductionResult production(OrderStatus status) {
        return new OrderProductionUseCase.ProductionResult(200L, status, LocalDate.of(2026, 5, 8));
    }

    private static OrderPickupUseCase.PickupResult pickup(OrderStatus status) {
        return new OrderPickupUseCase.PickupResult(200L, status, LocalDateTime.of(2026, 5, 10, 21, 0));
    }

    private static OrderShippingUseCase.ShippingResult shipping(OrderStatus status) {
        return new OrderShippingUseCase.ShippingResult(
                200L,
                status,
                LocalDate.of(2026, 5, 8),
                "CJ대한통운",
                "1234567890",
                ShippingCarrier.CJ_LOGISTICS,
                TrackingRegistrationStatus.ACTIVE,
                ShipmentTrackingStatus.IN_TRANSIT,
                "배송중",
                LocalDateTime.of(2026, 5, 2, 9, 0));
    }

    private static OrderHistoryResponse orderHistory() {
        return new OrderHistoryResponse(1L, OrderApprovalDecision.APPROVE, ADMIN_USER_ID,
                "정상 승인", LocalDateTime.of(2026, 5, 1, 21, 5));
    }

    private static BatchResult batchResult() {
        return new BatchResult(1, 0, Map.of());
    }
}
