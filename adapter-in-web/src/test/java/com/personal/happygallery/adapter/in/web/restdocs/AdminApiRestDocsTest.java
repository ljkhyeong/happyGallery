package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminBookingController;
import com.personal.happygallery.adapter.in.web.admin.AdminClassController;
import com.personal.happygallery.adapter.in.web.admin.AdminCredentialController;
import com.personal.happygallery.adapter.in.web.admin.AdminDashboardController;
import com.personal.happygallery.adapter.in.web.admin.AdminInquiryController;
import com.personal.happygallery.adapter.in.web.admin.AdminLoginController;
import com.personal.happygallery.adapter.in.web.admin.AdminNoticeController;
import com.personal.happygallery.adapter.in.web.admin.AdminNotificationController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderApprovalController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderPickupController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderProductionController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderQueryController;
import com.personal.happygallery.adapter.in.web.admin.AdminOrderShippingController;
import com.personal.happygallery.adapter.in.web.admin.AdminPassController;
import com.personal.happygallery.adapter.in.web.admin.AdminPaymentReconciliationController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductQnaController;
import com.personal.happygallery.adapter.in.web.admin.AdminRefundController;
import com.personal.happygallery.adapter.in.web.admin.AdminSetupController;
import com.personal.happygallery.adapter.in.web.admin.AdminSlotController;
import com.personal.happygallery.adapter.in.web.admin.AdminWorkshopProfileController;
import com.personal.happygallery.adapter.in.web.admin.LocalPhoneVerificationController;
import com.personal.happygallery.adapter.in.web.admin.LocalRefundFailureController;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase;
import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.booking.port.in.BookingNoShowUseCase;
import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotItem;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotResult;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotStatus;
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
import com.personal.happygallery.application.notification.port.in.NotificationFailureAdminUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.application.order.port.in.AdminOrderFulfillmentResponse;
import com.personal.happygallery.application.order.port.in.OrderApprovalUseCase;
import com.personal.happygallery.application.order.port.in.OrderHistoryResponse;
import com.personal.happygallery.application.order.port.in.OrderPickupUseCase;
import com.personal.happygallery.application.order.port.in.OrderProductionUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.application.payment.port.in.DevRefundFailureUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.application.payment.port.in.RefundQueryUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;

    private AdminAuthUseCase adminAuthUseCase;
    private AdminCredentialUseCase adminCredentialUseCase;
    private AdminSetupUseCase adminSetupUseCase;
    private ProductAdminUseCase productAdminUseCase;
    private ProductQueryUseCase productQueryUseCase;
    private ClassManagementUseCase classManagementUseCase;
    private ClassQueryUseCase classQueryUseCase;
    private SlotManagementUseCase slotManagementUseCase;
    private SlotQueryUseCase slotQueryUseCase;
    private AdminBookingQueryUseCase adminBookingQueryUseCase;
    private AdminBookingSearchUseCase adminBookingSearchUseCase;
    private BookingNoShowUseCase bookingNoShowUseCase;
    private BookingSettlementUseCase bookingSettlementUseCase;
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
    private RefundQueryUseCase refundQueryUseCase;
    private NotificationFailureAdminUseCase notificationFailureAdminUseCase;
    private PaymentReconciliationAdminUseCase paymentReconciliationAdminUseCase;
    private ProductQnaUseCase qnaUseCase;
    private InquiryUseCase inquiryUseCase;
    private PassExpiryBatchUseCase passExpiryBatchUseCase;
    private PassRefundUseCase passRefundUseCase;
    private DevPhoneVerificationQueryUseCase phoneVerificationQueryUseCase;
    private DevRefundFailureUseCase devRefundFailureUseCase;
    private WorkshopProfileUseCase workshopProfileUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        adminAuthUseCase = mock(AdminAuthUseCase.class);
        adminCredentialUseCase = mock(AdminCredentialUseCase.class);
        adminSetupUseCase = mock(AdminSetupUseCase.class);
        productAdminUseCase = mock(ProductAdminUseCase.class);
        productQueryUseCase = mock(ProductQueryUseCase.class);
        classManagementUseCase = mock(ClassManagementUseCase.class);
        classQueryUseCase = mock(ClassQueryUseCase.class);
        slotManagementUseCase = mock(SlotManagementUseCase.class);
        slotQueryUseCase = mock(SlotQueryUseCase.class);
        adminBookingQueryUseCase = mock(AdminBookingQueryUseCase.class);
        adminBookingSearchUseCase = mock(AdminBookingSearchUseCase.class);
        bookingNoShowUseCase = mock(BookingNoShowUseCase.class);
        bookingSettlementUseCase = mock(BookingSettlementUseCase.class);
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
        refundQueryUseCase = mock(RefundQueryUseCase.class);
        notificationFailureAdminUseCase = mock(NotificationFailureAdminUseCase.class);
        paymentReconciliationAdminUseCase = mock(PaymentReconciliationAdminUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);
        passExpiryBatchUseCase = mock(PassExpiryBatchUseCase.class);
        passRefundUseCase = mock(PassRefundUseCase.class);
        phoneVerificationQueryUseCase = mock(DevPhoneVerificationQueryUseCase.class);
        devRefundFailureUseCase = mock(DevRefundFailureUseCase.class);
        workshopProfileUseCase = mock(WorkshopProfileUseCase.class);

        ProductQueryUseCase.ProductWithInventory product = RestDocsFixtures.productWithInventory();
        ProductQueryUseCase.ProductWithInventory inactiveProduct =
                RestDocsFixtures.productWithInventory(ProductStatus.INACTIVE);
        InventoryAdjustment inventoryAdjustment = inventoryAdjustment();
        BookingClass bookingClass = RestDocsFixtures.bookingClass();
        Slot slot = RestDocsFixtures.slot();
        Booking booking = RestDocsFixtures.booking();
        Order order = RestDocsFixtures.order();
        Refund orderRefund = RestDocsFixtures.orderRefund();
        Notice notice = RestDocsFixtures.notice();
        ProductQnaUseCase.QnaWithAuthor qna = qna();
        InquiryUseCase.InquiryWithUser inquiry = inquiry();
        when(adminAuthUseCase.login("admin", "admin123456")).thenReturn("admin-session-token");
        when(adminSetupUseCase.isAvailable()).thenReturn(true);
        when(productAdminUseCase.register(any(), any(), any(), anyLong(), anyInt(), any(), any()))
                .thenReturn(new ProductAdminUseCase.ProductInventoryResult(
                        product.product(), product.inventory()));
        when(productQueryUseCase.listAllProducts()).thenReturn(List.of(product));
        when(productAdminUseCase.update(eq(1L), any(), any(), anyLong(), any(), any()))
                .thenReturn(new ProductAdminUseCase.ProductInventoryResult(
                        product.product(), product.inventory()));
        when(productAdminUseCase.changeStatus(1L, ProductStatus.INACTIVE))
                .thenReturn(new ProductAdminUseCase.ProductInventoryResult(
                        inactiveProduct.product(), inactiveProduct.inventory()));
        when(productAdminUseCase.adjustInventory(any())).thenReturn(inventoryAdjustment);
        when(productAdminUseCase.listRecentInventoryAdjustments(1L))
                .thenReturn(List.of(inventoryAdjustment));
        when(classManagementUseCase.createClass(any()))
                .thenReturn(bookingClass);
        when(classQueryUseCase.listAll()).thenReturn(List.of(bookingClass));
        when(slotQueryUseCase.listByClass(1L)).thenReturn(List.of(slot));
        when(slotManagementUseCase.createSlot(any(), any())).thenReturn(slot);
        when(slotManagementUseCase.previewBulkSlots(any())).thenReturn(new BulkSlotResult(List.of(
                new BulkSlotItem(
                        null,
                        LocalDateTime.of(2026, 5, 7, 19, 0),
                        LocalDateTime.of(2026, 5, 7, 21, 0),
                        BulkSlotStatus.CREATABLE,
                        false))));
        when(slotManagementUseCase.createBulkSlots(any())).thenReturn(new BulkSlotResult(List.of(
                new BulkSlotItem(
                        42L,
                        LocalDateTime.of(2026, 5, 7, 19, 0),
                        LocalDateTime.of(2026, 5, 7, 21, 0),
                        BulkSlotStatus.CREATED,
                        false))));
        when(slotManagementUseCase.deactivateSlot(42L)).thenReturn(slot);
        when(slotManagementUseCase.activateSlot(42L)).thenReturn(slot);
        when(adminBookingQueryUseCase.listBookings(any(), any())).thenReturn(List.of(adminBookingResponse()));
        when(adminBookingSearchUseCase.search(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminBookingSearchRow()), 0, 20, 1));
        when(bookingNoShowUseCase.markNoShow(100L)).thenReturn(booking);
        when(bookingSettlementUseCase.markBalancePaid(100L)).thenReturn(booking);
        when(bookingSettlementUseCase.updateArrears(eq(100L), anyBoolean())).thenReturn(booking);
        when(bookingSettlementUseCase.complete(100L)).thenReturn(booking);
        when(adminOrderQueryUseCase.listOrders(any(), any(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(adminOrderResponse()), "cursor-next", true));
        when(adminOrderQueryUseCase.getFulfillment(200L)).thenReturn(adminOrderFulfillmentResponse());
        when(adminOrderSearchUseCase.search(any(), any(), any(), any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminOrderSearchRow()), 0, 20, 1));
        when(orderApprovalUseCase.reject(200L, ADMIN_USER_ID))
                .thenReturn(new OrderApprovalUseCase.RejectResult(order, orderRefund));
        when(orderProductionUseCase.resumeProduction(200L, ADMIN_USER_ID)).thenReturn(production(OrderStatus.IN_PRODUCTION));
        when(orderProductionUseCase.completeProduction(200L, ADMIN_USER_ID))
                .thenReturn(production(OrderStatus.APPROVED_FULFILLMENT_PENDING));
        when(orderProductionUseCase.setExpectedShipDate(eq(200L), any())).thenReturn(production(OrderStatus.IN_PRODUCTION));
        when(orderProductionUseCase.proposeDelay(200L))
                .thenReturn(production(OrderStatus.DELAY_CONSENT_PENDING));
        when(orderProductionUseCase.cancelForDelayRejection(200L, ADMIN_USER_ID))
                .thenReturn(new OrderProductionUseCase.DelayCancellationResult(
                        production(OrderStatus.DELAY_REJECTED_CANCELED), orderRefund));
        when(orderPickupUseCase.markPickupReady(eq(200L), any(), eq(ADMIN_USER_ID)))
                .thenReturn(pickup(OrderStatus.PICKUP_READY));
        when(orderPickupUseCase.confirmPickup(200L, ADMIN_USER_ID)).thenReturn(pickup(OrderStatus.PICKED_UP));
        when(orderShippingUseCase.prepareShipping(200L, ADMIN_USER_ID)).thenReturn(shipping(OrderStatus.SHIPPING_PREPARING));
        when(orderShippingUseCase.markShipped(
                200L, "CJ대한통운", "1234567890", ADMIN_USER_ID))
                .thenReturn(shipping(OrderStatus.SHIPPED));
        when(orderShippingUseCase.markDelivered(200L, ADMIN_USER_ID)).thenReturn(shipping(OrderStatus.DELIVERED));
        when(adminOrderQueryUseCase.getOrderHistory(200L)).thenReturn(List.of(orderHistory()));
        when(pickupExpireBatchUseCase.expirePickups()).thenReturn(batchResult());
        stubDashboard();
        when(noticeQueryUseCase.listAll()).thenReturn(List.of(notice));
        when(noticeQueryUseCase.getDetail(1L)).thenReturn(notice);
        when(noticeAdminUseCase.create(any(), any(), anyBoolean())).thenReturn(notice);
        when(noticeAdminUseCase.update(eq(1L), any(), any(), anyBoolean())).thenReturn(notice);
        when(refundRetryUseCase.listFailed(isNull(), anyInt()))
                .thenReturn(new CursorPage<>(List.of(), null, false));
        when(refundRetryUseCase.retry(anyLong())).thenReturn(orderRefund);
        when(refundQueryUseCase.getRefund(anyLong())).thenReturn(orderRefund);
        NotificationOutbox retriedNotification = NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        10L, NotificationEventType.PASS_PURCHASED, "PASS", 300L),
                LocalDateTime.of(2026, 5, 1, 21, 0));
        when(notificationFailureAdminUseCase.listFailed()).thenReturn(List.of());
        when(notificationFailureAdminUseCase.retry(1L)).thenReturn(retriedNotification);
        when(paymentReconciliationAdminUseCase.listRequired()).thenReturn(List.of());
        when(paymentReconciliationAdminUseCase.reconcile(1L)).thenReturn(
                new PaymentReconciliationAdminUseCase.ReconciliationResult(
                        1L,
                        PaymentAttemptStatus.CONFIRMED,
                        300L,
                        "PG 승인 확인 후 서비스 처리를 완료했습니다."));
        when(qnaUseCase.listByProduct(1L)).thenReturn(List.of(qna));
        when(qnaUseCase.replyAndGet(eq(5L), any(), eq(ADMIN_USER_ID))).thenReturn(qna);
        when(inquiryUseCase.listAll(isNull(), anyInt()))
                .thenReturn(new CursorPage<>(List.of(inquiry), null, false));
        when(inquiryUseCase.findByIdForAdmin(9L)).thenReturn(inquiry);
        when(inquiryUseCase.replyAndGet(eq(9L), any(), eq(ADMIN_USER_ID))).thenReturn(inquiry);
        when(passExpiryBatchUseCase.expireAll()).thenReturn(batchResult());
        when(passRefundUseCase.refundPass(300L))
                .thenReturn(new PassRefundUseCase.PassRefundResult(1, 7, 210000L, 900L, RefundStatus.REQUESTED));
        when(phoneVerificationQueryUseCase.findLatestUnverifiedCode("01012345678")).thenReturn(Optional.of("123456"));
        WorkshopProfile workshop = workshopProfile();
        when(workshopProfileUseCase.get()).thenReturn(workshop);
        when(workshopProfileUseCase.update(any())).thenReturn(workshop);

        mockMvc = mockMvc(restDocumentation,
                new AdminLoginController(adminAuthUseCase),
                new AdminCredentialController(adminCredentialUseCase),
                new AdminSetupController(new AdminSetupProperties("setup-token"), adminSetupUseCase),
                new AdminProductController(productAdminUseCase, productQueryUseCase),
                new AdminClassController(classManagementUseCase, classQueryUseCase),
                new AdminSlotController(slotManagementUseCase, slotQueryUseCase),
                new AdminBookingController(
                        adminBookingQueryUseCase,
                        adminBookingSearchUseCase,
                        bookingNoShowUseCase,
                        bookingSettlementUseCase),
                new AdminOrderQueryController(adminOrderQueryUseCase, adminOrderSearchUseCase),
                new AdminOrderApprovalController(orderApprovalUseCase),
                new AdminOrderProductionController(orderProductionUseCase),
                new AdminOrderPickupController(orderPickupUseCase, pickupExpireBatchUseCase),
                new AdminOrderShippingController(orderShippingUseCase),
                new AdminDashboardController(dashboardQueryUseCase),
                new AdminNoticeController(noticeAdminUseCase, noticeQueryUseCase),
                new AdminWorkshopProfileController(workshopProfileUseCase),
                new AdminRefundController(refundRetryUseCase, refundQueryUseCase),
                new AdminNotificationController(notificationFailureAdminUseCase),
                new AdminPaymentReconciliationController(paymentReconciliationAdminUseCase),
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
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123456\"}"))
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
    @DisplayName("관리자 비밀번호 변경 API를 문서화한다")
    void admin_change_password() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/auth/password")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "admin123456",
                                  "newPassword": "new-admin-123456"
                                }
                                """))
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
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "setup-token",
                                  "username": "admin",
                                  "password": "admin123456"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 상품 등록 API를 문서화한다")
    void admin_create_product() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "시그니처 캔들",
                                  "type": "READY_STOCK",
                                  "category": "CANDLE",
                                  "price": 39000,
                                  "quantity": 12
                                }
                                """))
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
    @DisplayName("관리자 공방 방문 정보 수정 API를 문서화한다")
    void admin_update_workshop_profile() throws Exception {
        mockMvc.perform(put("/api/v1/admin/workshop")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "해피갤러리",
                                  "phone": "02-123-4567",
                                  "postalCode": "01234",
                                  "addressLine1": "서울시 종로구 공방길 1",
                                  "addressLine2": "2층",
                                  "businessHours": "화-일 10:00-19:00",
                                  "mapUrl": "https://map.example.com/happygallery",
                                  "parkingInfo": "근처 공영주차장 이용"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 상품 콘텐츠 수정 API를 문서화한다")
    void admin_update_product() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/products/{id}", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "시그니처 캔들",
                                  "category": "CANDLE",
                                  "price": 42000,
                                  "description": "소이 왁스로 만든 작품",
                                  "imageUrl": "https://images.example.com/candle.jpg"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 상품 상태 변경 API를 문서화한다")
    void admin_change_product_status() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/products/{id}/status", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 재고 수동 조정 API를 문서화한다")
    void admin_adjust_inventory() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products/{id}/inventory-adjustments", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "DECREASE",
                                  "quantity": 2,
                                  "reason": "오프라인 매장 판매"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 재고 조정 이력 API를 문서화한다")
    void admin_list_inventory_adjustments() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/{id}/inventory-adjustments", 1L)
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
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "향수 원데이",
                                  "category": "PERFUME",
                                  "durationMin": 120,
                                  "price": 50000,
                                  "bufferMin": 30,
                                  "passEligible": false,
                                  "description": "향을 조합해 나만의 향수를 만듭니다.",
                                  "preparationInfo": "편한 복장",
                                  "targetAudience": "향수 만들기가 처음인 분"
                                }
                                """))
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
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": 1,
                                  "startAt": "2026-05-07T19:00:00"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 슬롯 일괄 미리보기 API를 문서화한다")
    void admin_preview_bulk_slots() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots/bulk/preview")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content(bulkSlotRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatableCount").value(1));
    }

    @Test
    @DisplayName("관리자 슬롯 일괄 생성 API를 문서화한다")
    void admin_create_bulk_slots() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots/bulk")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content(bulkSlotRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(1));
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
    @DisplayName("관리자 슬롯 활성화 API를 문서화한다")
    void admin_activate_slot() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/slots/{id}/activate", 42L)
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
    @DisplayName("관리자 예약 잔금 결제 API를 문서화한다")
    void admin_mark_booking_balance_paid() throws Exception {
        mockMvc.perform(post("/api/v1/admin/bookings/{bookingId}/balance-payment", 100L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
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
                        .content("{\"carrier\":\"CJ대한통운\",\"trackingNumber\":\"1234567890\"}"))
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
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운영 안내",
                                  "content": "5월 클래스 운영 안내입니다.",
                                  "pinned": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 공지 수정 API를 문서화한다")
    void admin_update_notice() throws Exception {
        mockMvc.perform(put("/api/v1/admin/notices/{id}", 1L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운영 안내",
                                  "content": "수정된 안내입니다.",
                                  "pinned": false
                                }
                                """))
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("관리자 환불 상태 조회 API를 문서화한다")
    void admin_get_refund() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds/{refundId}", 901L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(901))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("관리자 환불 재시도 API를 문서화한다")
    void admin_retry_refund() throws Exception {
        mockMvc.perform(post("/api/v1/admin/refunds/{refundId}/retry", 1L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(901))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("관리자 실패 알림 목록 API를 문서화한다")
    void admin_list_failed_notifications() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notifications/failed").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 실패 알림 재처리 API를 문서화한다")
    void admin_retry_notification() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/{outboxId}/retry", 1L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("관리자 결제 대사 대상 목록 API를 문서화한다")
    void admin_list_payment_reconciliations() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payment-attempts/reconciliation-required").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 결제 대사 실행 API를 문서화한다")
    void admin_reconcile_payment() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payment-attempts/{attemptId}/reconcile", 1L)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.domainId").value(300L));
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
                        .contentType(APPLICATION_JSON)
                        .content("{\"replyContent\":\"주문 승인 후 안내드립니다.\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 문의 목록 API를 문서화한다")
    void admin_list_inquiries() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inquiries").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.hasMore").value(false));
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
                        .contentType(APPLICATION_JSON)
                        .content("{\"replyContent\":\"마이페이지에서 변경할 수 있습니다.\"}"))
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
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"로컬 smoke 강제 환불 실패\"}"))
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
                LocalDateTime.of(2026, 5, 7, 21, 0), "BOOKED",
                5000L, LocalDateTime.of(2026, 5, 1, 20, 50),
                45000L, "UNPAID", null, false, false);
    }

    private static AdminBookingSearchRow adminBookingSearchRow() {
        return new AdminBookingSearchRow(100L, "BK-00000100", "GUEST", "홍길동", "01012345678",
                "향수 원데이", LocalDateTime.of(2026, 5, 7, 19, 0),
                LocalDateTime.of(2026, 5, 7, 21, 0), "BOOKED",
                5000L, LocalDateTime.of(2026, 5, 1, 20, 50),
                45000L, "UNPAID", null, false, false,
                LocalDateTime.of(2026, 5, 1, 20, 50).atOffset(ZoneOffset.UTC));
    }

    private static AdminOrderResponse adminOrderResponse() {
        return new AdminOrderResponse(200L, "ORD-00000200", "PAID_APPROVAL_PENDING", 39000L, 0L, "PICKUP",
                List.of(new AdminOrderResponse.Item(1L, "시그니처 캔들", 1, 39000L)),
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
                "1234567890");
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

    private static WorkshopProfile workshopProfile() {
        WorkshopProfile profile = new WorkshopProfile("해피갤러리");
        profile.update(
                "해피갤러리", "02-123-4567", "01234",
                "서울시 종로구 공방길 1", "2층", "화-일 10:00-19:00",
                "https://map.example.com/happygallery", "근처 공영주차장 이용",
                LocalDateTime.of(2026, 5, 1, 21, 0));
        return profile;
    }

    private static OrderPickupUseCase.PickupResult pickup(OrderStatus status) {
        return new OrderPickupUseCase.PickupResult(200L, status, LocalDateTime.of(2026, 5, 10, 21, 0));
    }

    private static OrderShippingUseCase.ShippingResult shipping(OrderStatus status) {
        return new OrderShippingUseCase.ShippingResult(
                200L, status, LocalDate.of(2026, 5, 8), "CJ대한통운", "1234567890");
    }

    private static OrderHistoryResponse orderHistory() {
        return new OrderHistoryResponse(1L, OrderApprovalDecision.APPROVE, ADMIN_USER_ID,
                "정상 승인", LocalDateTime.of(2026, 5, 1, 21, 5));
    }

    private static InventoryAdjustment inventoryAdjustment() {
        InventoryAdjustment adjustment = mock(InventoryAdjustment.class);
        when(adjustment.getId()).thenReturn(10L);
        when(adjustment.getProductId()).thenReturn(1L);
        when(adjustment.getType()).thenReturn(InventoryAdjustmentType.DECREASE);
        when(adjustment.getQuantity()).thenReturn(2);
        when(adjustment.getQuantityBefore()).thenReturn(12);
        when(adjustment.getQuantityAfter()).thenReturn(10);
        when(adjustment.getReason()).thenReturn("오프라인 매장 판매");
        when(adjustment.getAdjustedByAdminId()).thenReturn(ADMIN_USER_ID);
        when(adjustment.getAdjustedBy()).thenReturn("admin");
        when(adjustment.getAdjustedAt()).thenReturn(LocalDateTime.of(2026, 5, 1, 21, 5));
        return adjustment;
    }

    private static String bulkSlotRequest() {
        return """
                {
                  "classId": 1,
                  "dateFrom": "2026-05-07",
                  "dateTo": "2026-05-31",
                  "weekdays": ["THURSDAY", "SATURDAY"],
                  "startTimes": ["10:00:00", "14:00:00"]
                }
                """;
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
