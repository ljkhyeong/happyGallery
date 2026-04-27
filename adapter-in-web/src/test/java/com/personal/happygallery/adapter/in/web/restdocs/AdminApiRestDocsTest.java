package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminBookingController;
import com.personal.happygallery.adapter.in.web.admin.AdminClassController;
import com.personal.happygallery.adapter.in.web.admin.AdminDashboardController;
import com.personal.happygallery.adapter.in.web.admin.AdminInquiryController;
import com.personal.happygallery.adapter.in.web.admin.AdminLoginController;
import com.personal.happygallery.adapter.in.web.admin.AdminNoticeController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderController;
import com.personal.happygallery.adapter.in.web.admin.AdminPassController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductQnaController;
import com.personal.happygallery.adapter.in.web.admin.AdminRefundController;
import com.personal.happygallery.adapter.in.web.admin.AdminSetupController;
import com.personal.happygallery.adapter.in.web.admin.AdminSlotController;
import com.personal.happygallery.adapter.in.web.admin.LocalPhoneVerificationController;
import com.personal.happygallery.adapter.in.web.admin.LocalRefundFailureController;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.booking.port.in.BookingNoShowUseCase;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.customer.port.in.DevPhoneVerificationQueryUseCase;
import com.personal.happygallery.application.dashboard.dto.DailyRevenue;
import com.personal.happygallery.application.dashboard.dto.DashboardOverview;
import com.personal.happygallery.application.dashboard.dto.Granularity;
import com.personal.happygallery.application.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.application.dashboard.dto.RefundStats;
import com.personal.happygallery.application.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import com.personal.happygallery.application.dashboard.dto.StatusCount;
import com.personal.happygallery.application.dashboard.dto.TopProduct;
import com.personal.happygallery.application.dashboard.port.in.DashboardQueryUseCase;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeAdminUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderHistoryResponse;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.application.payment.port.in.DevRefundFailureUseCase;
import com.personal.happygallery.application.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notice.Notice;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;

    private AdminAuthUseCase adminAuthUseCase;
    private AdminSetupUseCase adminSetupUseCase;
    private ProductAdminUseCase productAdminUseCase;
    private ProductQueryUseCase productQueryUseCase;
    private ClassManagementUseCase classManagementUseCase;
    private SlotManagementUseCase slotManagementUseCase;
    private SlotQueryUseCase slotQueryUseCase;
    private AdminBookingQueryUseCase adminBookingQueryUseCase;
    private AdminBookingSearchUseCase adminBookingSearchUseCase;
    private BookingNoShowUseCase bookingNoShowUseCase;
    private AdminOrderQueryUseCase adminOrderQueryUseCase;
    private AdminOrderSearchUseCase adminOrderSearchUseCase;
    private OrderApprovalUseCase orderApprovalUseCase;
    private OrderProductionUseCase orderProductionUseCase;
    private OrderPickupUseCase orderPickupUseCase;
    private OrderShippingUseCase orderShippingUseCase;
    private PickupExpireBatchUseCase pickupExpireBatchUseCase;
    private DashboardQueryUseCase dashboardQueryUseCase;
    private NoticeAdminUseCase noticeAdminUseCase;
    private NoticeQueryUseCase noticeQueryUseCase;
    private RefundRetryUseCase refundRetryUseCase;
    private ProductQnaUseCase qnaUseCase;
    private InquiryUseCase inquiryUseCase;
    private PassExpiryBatchUseCase passExpiryBatchUseCase;
    private PassRefundUseCase passRefundUseCase;
    private DevPhoneVerificationQueryUseCase phoneVerificationQueryUseCase;
    private DevRefundFailureUseCase devRefundFailureUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        adminAuthUseCase = mock(AdminAuthUseCase.class);
        adminSetupUseCase = mock(AdminSetupUseCase.class);
        productAdminUseCase = mock(ProductAdminUseCase.class);
        productQueryUseCase = mock(ProductQueryUseCase.class);
        classManagementUseCase = mock(ClassManagementUseCase.class);
        slotManagementUseCase = mock(SlotManagementUseCase.class);
        slotQueryUseCase = mock(SlotQueryUseCase.class);
        adminBookingQueryUseCase = mock(AdminBookingQueryUseCase.class);
        adminBookingSearchUseCase = mock(AdminBookingSearchUseCase.class);
        bookingNoShowUseCase = mock(BookingNoShowUseCase.class);
        adminOrderQueryUseCase = mock(AdminOrderQueryUseCase.class);
        adminOrderSearchUseCase = mock(AdminOrderSearchUseCase.class);
        orderApprovalUseCase = mock(OrderApprovalUseCase.class);
        orderProductionUseCase = mock(OrderProductionUseCase.class);
        orderPickupUseCase = mock(OrderPickupUseCase.class);
        orderShippingUseCase = mock(OrderShippingUseCase.class);
        pickupExpireBatchUseCase = mock(PickupExpireBatchUseCase.class);
        dashboardQueryUseCase = mock(DashboardQueryUseCase.class);
        noticeAdminUseCase = mock(NoticeAdminUseCase.class);
        noticeQueryUseCase = mock(NoticeQueryUseCase.class);
        refundRetryUseCase = mock(RefundRetryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);
        passExpiryBatchUseCase = mock(PassExpiryBatchUseCase.class);
        passRefundUseCase = mock(PassRefundUseCase.class);
        phoneVerificationQueryUseCase = mock(DevPhoneVerificationQueryUseCase.class);
        devRefundFailureUseCase = mock(DevRefundFailureUseCase.class);

        ProductQueryUseCase.ProductWithInventory product = RestDocsFixtures.productWithInventory();
        BookingClass bookingClass = RestDocsFixtures.bookingClass();
        Slot slot = RestDocsFixtures.slot();
        Booking booking = RestDocsFixtures.booking();
        Notice notice = RestDocsFixtures.notice();
        ProductQnaUseCase.QnaWithAuthor qna = qna();
        InquiryUseCase.InquiryWithUser inquiry = inquiry();
        when(adminAuthUseCase.login("admin", "admin123456")).thenReturn("admin-session-token");
        when(adminSetupUseCase.isAvailable()).thenReturn(true);
        when(productAdminUseCase.register(any(), any(), any(), anyLong(), anyInt()))
                .thenReturn(new ProductAdminUseCase.RegisterResult(product.product(), product.inventory()));
        when(productQueryUseCase.listActiveProducts()).thenReturn(List.of(product));
        when(classManagementUseCase.createClass(any(), any(), anyInt(), anyLong(), anyInt()))
                .thenReturn(bookingClass);
        when(slotQueryUseCase.listByClass(1L)).thenReturn(List.of(slot));
        when(slotManagementUseCase.createSlot(any(), any(), any())).thenReturn(slot);
        when(slotManagementUseCase.deactivateSlot(42L)).thenReturn(slot);
        when(adminBookingQueryUseCase.listBookings(any(), any())).thenReturn(List.of(adminBookingResponse()));
        when(adminBookingSearchUseCase.search(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminBookingSearchRow()), 0, 20, 1));
        when(bookingNoShowUseCase.markNoShow(100L)).thenReturn(booking);
        when(adminOrderQueryUseCase.listOrders(any(), any(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(adminOrderResponse()), "cursor-next", true));
        when(adminOrderSearchUseCase.search(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminOrderSearchRow()), 0, 20, 1));
        when(orderProductionUseCase.resumeProduction(200L, ADMIN_USER_ID)).thenReturn(production(OrderStatus.IN_PRODUCTION));
        when(orderProductionUseCase.completeProduction(200L, ADMIN_USER_ID))
                .thenReturn(production(OrderStatus.APPROVED_FULFILLMENT_PENDING));
        when(orderProductionUseCase.setExpectedShipDate(eq(200L), any())).thenReturn(production(OrderStatus.IN_PRODUCTION));
        when(orderProductionUseCase.requestDelay(200L)).thenReturn(production(OrderStatus.DELAY_REQUESTED));
        when(orderPickupUseCase.markPickupReady(eq(200L), any())).thenReturn(pickup(OrderStatus.PICKUP_READY));
        when(orderPickupUseCase.confirmPickup(200L)).thenReturn(pickup(OrderStatus.PICKED_UP));
        when(orderShippingUseCase.prepareShipping(200L, ADMIN_USER_ID)).thenReturn(shipping(OrderStatus.SHIPPING_PREPARING));
        when(orderShippingUseCase.markShipped(200L, ADMIN_USER_ID)).thenReturn(shipping(OrderStatus.SHIPPED));
        when(orderShippingUseCase.markDelivered(200L, ADMIN_USER_ID)).thenReturn(shipping(OrderStatus.DELIVERED));
        when(adminOrderQueryUseCase.getOrderHistory(200L)).thenReturn(List.of(orderHistory()));
        when(pickupExpireBatchUseCase.expirePickups()).thenReturn(batchResult());
        stubDashboard();
        when(noticeQueryUseCase.listAll()).thenReturn(List.of(notice));
        when(noticeQueryUseCase.getDetail(1L)).thenReturn(notice);
        when(noticeAdminUseCase.create(any(), any(), anyBoolean())).thenReturn(notice);
        when(noticeAdminUseCase.update(eq(1L), any(), any(), anyBoolean())).thenReturn(notice);
        when(refundRetryUseCase.listFailed()).thenReturn(List.of());
        when(qnaUseCase.listByProduct(1L)).thenReturn(List.of(qna));
        when(qnaUseCase.replyAndGet(eq(5L), any(), eq(ADMIN_USER_ID))).thenReturn(qna);
        when(inquiryUseCase.listAll()).thenReturn(List.of(inquiry));
        when(inquiryUseCase.findByIdForAdmin(9L)).thenReturn(inquiry);
        when(inquiryUseCase.replyAndGet(eq(9L), any(), eq(ADMIN_USER_ID))).thenReturn(inquiry);
        when(passExpiryBatchUseCase.expireAll()).thenReturn(batchResult());
        when(passRefundUseCase.refundPass(300L)).thenReturn(new PassRefundUseCase.PassRefundResult(1, 7, 210000L));
        when(phoneVerificationQueryUseCase.findLatestUnverifiedCode("01012345678")).thenReturn(Optional.of("123456"));

        mockMvc = mockMvc(restDocumentation,
                new AdminLoginController(adminAuthUseCase),
                new AdminSetupController(new AdminSetupProperties("setup-token"), adminSetupUseCase),
                new AdminProductController(productAdminUseCase, productQueryUseCase),
                new AdminClassController(classManagementUseCase),
                new AdminSlotController(slotManagementUseCase, slotQueryUseCase),
                new AdminBookingController(adminBookingQueryUseCase, adminBookingSearchUseCase, bookingNoShowUseCase),
                new AdminOrderController(adminOrderQueryUseCase, adminOrderSearchUseCase, orderApprovalUseCase,
                        orderProductionUseCase, orderPickupUseCase, orderShippingUseCase, pickupExpireBatchUseCase),
                new AdminDashboardController(dashboardQueryUseCase),
                new AdminNoticeController(noticeAdminUseCase, noticeQueryUseCase),
                new AdminRefundController(refundRetryUseCase),
                new AdminProductQnaController(qnaUseCase),
                new AdminInquiryController(inquiryUseCase),
                new AdminPassController(passExpiryBatchUseCase, passRefundUseCase),
                new LocalPhoneVerificationController(phoneVerificationQueryUseCase),
                new LocalRefundFailureController(devRefundFailureUseCase));
    }

    @Test
    @DisplayName("관리자 로그인 API를 문서화한다")
    void admin_login() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(jsonContent())
                        .content(json("{\"username\":\"admin\",\"password\":\"admin123456\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 로그아웃 API를 문서화한다")
    void admin_logout() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/logout")
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 최초 설정 상태 API를 문서화한다")
    void admin_setup_status() throws Exception {
        mockMvc.perform(get("/api/v1/admin/setup/status"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 최초 설정 API를 문서화한다")
    void admin_setup() throws Exception {
        mockMvc.perform(post("/api/v1/admin/setup")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "token": "setup-token",
                                  "username": "admin",
                                  "password": "admin123456"
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 상품 등록 API를 문서화한다")
    void admin_create_product() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "name": "시그니처 캔들",
                                  "type": "READY_STOCK",
                                  "category": "CANDLE",
                                  "price": 39000,
                                  "quantity": 12
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 상품 목록 API를 문서화한다")
    void admin_list_products() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 클래스 생성 API를 문서화한다")
    void admin_create_class() throws Exception {
        mockMvc.perform(post("/api/v1/admin/classes")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "name": "향수 원데이",
                                  "category": "PERFUME",
                                  "durationMin": 120,
                                  "price": 50000,
                                  "bufferMin": 30
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 슬롯 목록 API를 문서화한다")
    void admin_list_slots() throws Exception {
        mockMvc.perform(get("/api/v1/admin/slots")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("classId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 슬롯 생성 API를 문서화한다")
    void admin_create_slot() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "classId": 1,
                                  "startAt": "2026-05-07T19:00:00",
                                  "endAt": "2026-05-07T21:00:00"
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 슬롯 비활성화 API를 문서화한다")
    void admin_deactivate_slot() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/slots/{id}/deactivate", 42L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 예약 목록 API를 문서화한다")
    void admin_list_bookings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/bookings")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("date", "2026-05-07")
                        .param("status", "BOOKED"))
                .andExpect(status().isOk());
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
                .andExpect(status().isOk());
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
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 제작 재개 API를 문서화한다")
    void admin_resume_order_production() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/resume-production", 200L)
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
                        .contentType(jsonContent())
                        .content(json("{\"expectedShipDate\":\"2026-05-08\"}")))
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
    @DisplayName("관리자 주문 픽업 준비 API를 문서화한다")
    void admin_prepare_pickup() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/{id}/prepare-pickup", 200L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(jsonContent())
                        .content(json("{\"pickupDeadlineAt\":\"2026-05-10T21:00:00\"}")))
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
                        .header("Authorization", "Bearer admin-session-token"))
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

    @Test
    @DisplayName("관리자 대시보드 요약 API를 문서화한다")
    void admin_dashboard_overview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/overview")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 매출 요약 API를 문서화한다")
    void admin_dashboard_sales_summary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/sales-summary")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .param("granularity", "DAILY"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 매출 분해 API를 문서화한다")
    void admin_dashboard_revenue_breakdown() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/revenue-breakdown")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 상태 분포 API를 문서화한다")
    void admin_dashboard_order_status() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/order-status").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 환불 통계 API를 문서화한다")
    void admin_dashboard_refunds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/refunds")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 인기 상품 API를 문서화한다")
    void admin_dashboard_top_products() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/top-products")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .param("limit", "10")
                        .param("sort", "REVENUE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 일별 매출 API를 문서화한다")
    void admin_dashboard_daily_revenue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/daily-revenue")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 슬롯 이용률 API를 문서화한다")
    void admin_dashboard_slot_utilization() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/slot-utilization")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 공지 목록 API를 문서화한다")
    void admin_list_notices() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notices").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 공지 생성 API를 문서화한다")
    void admin_create_notice() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notices")
                        .with(adminUser())
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "title": "운영 안내",
                                  "content": "5월 클래스 운영 안내입니다.",
                                  "pinned": true
                                }
                                """)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 공지 수정 API를 문서화한다")
    void admin_update_notice() throws Exception {
        mockMvc.perform(put("/api/v1/admin/notices/{id}", 1L)
                        .with(adminUser())
                        .contentType(jsonContent())
                        .content(json("""
                                {
                                  "title": "운영 안내",
                                  "content": "수정된 안내입니다.",
                                  "pinned": false
                                }
                                """)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 공지 삭제 API를 문서화한다")
    void admin_delete_notice() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/notices/{id}", 1L).with(adminUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 실패 환불 목록 API를 문서화한다")
    void admin_list_failed_refunds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds/failed").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 환불 재시도 API를 문서화한다")
    void admin_retry_refund() throws Exception {
        mockMvc.perform(post("/api/v1/admin/refunds/{refundId}/retry", 1L).with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 QNA 목록 API를 문서화한다")
    void admin_list_qna() throws Exception {
        mockMvc.perform(get("/api/v1/admin/qna")
                        .with(adminUser())
                        .param("productId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 QNA 답변 API를 문서화한다")
    void admin_reply_qna() throws Exception {
        mockMvc.perform(post("/api/v1/admin/qna/{id}/reply", 5L)
                        .with(adminUser())
                        .contentType(jsonContent())
                        .content(json("{\"replyContent\":\"주문 승인 후 안내드립니다.\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 문의 목록 API를 문서화한다")
    void admin_list_inquiries() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inquiries").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 문의 상세 API를 문서화한다")
    void admin_get_inquiry() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inquiries/{id}", 9L).with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 문의 답변 API를 문서화한다")
    void admin_reply_inquiry() throws Exception {
        mockMvc.perform(post("/api/v1/admin/inquiries/{id}/reply", 9L)
                        .with(adminUser())
                        .contentType(jsonContent())
                        .content(json("{\"replyContent\":\"마이페이지에서 변경할 수 있습니다.\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 8회권 만료 배치 API를 문서화한다")
    void admin_expire_passes() throws Exception {
        mockMvc.perform(post("/api/v1/admin/passes/expire").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 8회권 환불 API를 문서화한다")
    void admin_refund_pass() throws Exception {
        mockMvc.perform(post("/api/v1/admin/passes/{passId}/refund", 300L).with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 최신 휴대폰 인증 코드 조회 API를 문서화한다")
    void local_latest_phone_verification_code() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dev/phone-verifications/latest")
                        .with(adminUser())
                        .param("phone", "01012345678"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 다음 환불 실패 설정 API를 문서화한다")
    void local_arm_next_refund_failure() throws Exception {
        mockMvc.perform(post("/api/v1/admin/dev/payment/refunds/fail-next")
                        .with(adminUser())
                        .contentType(jsonContent())
                        .content(json("{\"reason\":\"로컬 smoke 강제 환불 실패\"}")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 다음 환불 실패 해제 API를 문서화한다")
    void local_clear_next_refund_failure() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/dev/payment/refunds/fail-next").with(adminUser()))
                .andExpect(status().isNoContent());
    }

    private static AdminBookingResponse adminBookingResponse() {
        return new AdminBookingResponse(100L, "BK-00000100", "GUEST", "홍길동", "01012345678",
                "향수 원데이", LocalDateTime.of(2026, 5, 7, 19, 0),
                LocalDateTime.of(2026, 5, 7, 21, 0), "BOOKED", 5000L, 45000L, false);
    }

    private static AdminBookingSearchRow adminBookingSearchRow() {
        return new AdminBookingSearchRow(100L, "BK-00000100", "GUEST", "홍길동", "01012345678",
                "향수 원데이", LocalDateTime.of(2026, 5, 7, 19, 0),
                LocalDateTime.of(2026, 5, 7, 21, 0), "BOOKED", 5000L, 45000L, false,
                LocalDateTime.of(2026, 5, 1, 20, 50));
    }

    private static AdminOrderResponse adminOrderResponse() {
        return new AdminOrderResponse(200L, "ORD-00000200", "PAID_APPROVAL_PENDING", 39000L,
                LocalDateTime.of(2026, 5, 1, 20, 55),
                LocalDateTime.of(2026, 5, 1, 21, 15),
                LocalDateTime.of(2026, 5, 1, 20, 50));
    }

    private static AdminOrderSearchRow adminOrderSearchRow() {
        return new AdminOrderSearchRow(200L, "ORD-00000200", "PAID_APPROVAL_PENDING", 39000L,
                "홍길동", "01012345678",
                LocalDateTime.of(2026, 5, 1, 20, 55),
                LocalDateTime.of(2026, 5, 1, 21, 15),
                LocalDateTime.of(2026, 5, 1, 20, 50));
    }

    private static OrderProductionUseCase.ProductionResult production(OrderStatus status) {
        return new OrderProductionUseCase.ProductionResult(200L, status, LocalDate.of(2026, 5, 8));
    }

    private static OrderPickupUseCase.PickupResult pickup(OrderStatus status) {
        return new OrderPickupUseCase.PickupResult(200L, status, LocalDateTime.of(2026, 5, 10, 21, 0));
    }

    private static OrderShippingUseCase.ShippingResult shipping(OrderStatus status) {
        return new OrderShippingUseCase.ShippingResult(200L, status, LocalDate.of(2026, 5, 8));
    }

    private static OrderHistoryResponse orderHistory() {
        return new OrderHistoryResponse(1L, OrderApprovalDecision.APPROVE, ADMIN_USER_ID,
                "정상 승인", LocalDateTime.of(2026, 5, 1, 21, 5));
    }

    private static BatchResult batchResult() {
        return new BatchResult(1, 0, Map.of());
    }

    private static ProductQnaUseCase.QnaWithAuthor qna() {
        return new ProductQnaUseCase.QnaWithAuthor(RestDocsFixtures.productQna(), "홍길동");
    }

    private static InquiryUseCase.InquiryWithUser inquiry() {
        return new InquiryUseCase.InquiryWithUser(RestDocsFixtures.inquiry(), "홍길동");
    }

    private void stubDashboard() {
        when(dashboardQueryUseCase.getOverview(any(), any())).thenReturn(
                new DashboardOverview(39000L, 1, 1, 2, 120000L, 3));
        when(dashboardQueryUseCase.getSalesSummary(any(), any(), eq(Granularity.DAILY))).thenReturn(
                List.of(new PeriodSalesSummary("2026-05-01", 39000L, 1, 39000L)));
        when(dashboardQueryUseCase.getRevenueBreakdown(any(), any())).thenReturn(
                new RevenueBreakdown(39000L, 5000L, 45000L, 240000L, 329000L));
        when(dashboardQueryUseCase.getOrderStatusDistribution()).thenReturn(
                List.of(new StatusCount("PAID_APPROVAL_PENDING", 1)));
        when(dashboardQueryUseCase.getRefundStats(any(), any())).thenReturn(
                new RefundStats(1, 5000L, 0.1));
        when(dashboardQueryUseCase.getTopProducts(any(), any(), eq(10), any())).thenReturn(
                List.of(new TopProduct(1L, "시그니처 캔들", "READY_STOCK", 39000L, 1)));
        when(dashboardQueryUseCase.getDailyRevenueSeries(any(), any())).thenReturn(
                List.of(new DailyRevenue(LocalDate.of(2026, 5, 1), 39000L)));
        when(dashboardQueryUseCase.getSlotUtilization(any(), any())).thenReturn(
                List.of(new SlotUtilization(LocalDate.of(2026, 5, 7), "향수 원데이", 8, 2, 0.25)));
    }
}
