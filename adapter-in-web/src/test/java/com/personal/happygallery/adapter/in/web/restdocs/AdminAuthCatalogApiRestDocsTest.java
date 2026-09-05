package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminClassController;
import com.personal.happygallery.adapter.in.web.admin.AdminRestockDemandController;
import com.personal.happygallery.application.product.port.in.RestockDemandUseCase;
import com.personal.happygallery.adapter.in.web.admin.AdminCredentialController;
import com.personal.happygallery.adapter.in.web.admin.AdminLoginController;
import com.personal.happygallery.adapter.in.web.admin.AdminMediaController;
import com.personal.happygallery.adapter.in.web.admin.AdminMfaController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductController;
import com.personal.happygallery.adapter.in.web.admin.AdminSetupController;
import com.personal.happygallery.adapter.in.web.admin.AdminSlotController;
import com.personal.happygallery.adapter.in.web.admin.AdminSmartStoreOrderController;
import com.personal.happygallery.adapter.in.web.admin.AdminSmartStoreInquiryController;
import com.personal.happygallery.adapter.in.web.admin.AdminWorkshopProfileController;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.adapter.in.web.security.admin.AdminBearerTokenResolver;
import com.personal.happygallery.application.admin.port.AdminAuthenticationMethod;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase.LoginResult;
import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase.MfaEnrollment;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase.MfaStatus;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase.RecoveryCodes;
import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.booking.port.in.ClassManagementUseCase;
import com.personal.happygallery.application.booking.port.in.ClassQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.CalendarDay;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.CalendarView;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.DayOverrideMode;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ActionHistoryResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderDetailResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.DeliveryInfo;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ClaimDetail;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.CurrentOrderStatusResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ReturnDeliveryCompanyResult;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.CustomerInquiryResult;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.InquiryResult;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.AnswerTemplateResult;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.ProductOptions;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingHistoryResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ProductPreviewResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.CatalogPageResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.CatalogProductResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ChannelOptionResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.ChannelProductResult;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import com.personal.happygallery.domain.booking.BookingTimeBlock;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.domain.product.SmartStoreInventoryMappingAction;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreInventoryResolutionAction;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionStatus;
import com.personal.happygallery.domain.order.SmartStoreOrderReconciliationOutcome;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAuthCatalogApiRestDocsTest extends RestDocsTestSupport {

    private static final String SNIPPET_GROUP = "admin-api-rest-docs-test";

    private MockMvc mockMvc;
    private AdminAuthUseCase adminAuthUseCase;
    private AdminCredentialUseCase adminCredentialUseCase;
    private AdminMfaUseCase adminMfaUseCase;
    private AdminSetupUseCase adminSetupUseCase;
    private ProductAdminUseCase productAdminUseCase;
    private ProductQueryUseCase productQueryUseCase;
    private SmartStoreInventoryUseCase smartStoreInventoryUseCase;
    private SmartStoreChannelOrderUseCase smartStoreChannelOrderUseCase;
    private SmartStoreInquiryUseCase smartStoreInquiryUseCase;
    private WorkshopProfileUseCase workshopProfileUseCase;
    private ClassManagementUseCase classManagementUseCase;
    private ClassQueryUseCase classQueryUseCase;
    private SlotManagementUseCase slotManagementUseCase;
    private SlotQueryUseCase slotQueryUseCase;
    private BookingCalendarUseCase bookingCalendarUseCase;
    private ImageMediaUseCase imageMediaUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        adminAuthUseCase = mock(AdminAuthUseCase.class);
        adminCredentialUseCase = mock(AdminCredentialUseCase.class);
        adminMfaUseCase = mock(AdminMfaUseCase.class);
        adminSetupUseCase = mock(AdminSetupUseCase.class);
        productAdminUseCase = mock(ProductAdminUseCase.class);
        productQueryUseCase = mock(ProductQueryUseCase.class);
        smartStoreInventoryUseCase = mock(SmartStoreInventoryUseCase.class);
        smartStoreChannelOrderUseCase = mock(SmartStoreChannelOrderUseCase.class);
        smartStoreInquiryUseCase = mock(SmartStoreInquiryUseCase.class);
        workshopProfileUseCase = mock(WorkshopProfileUseCase.class);
        classManagementUseCase = mock(ClassManagementUseCase.class);
        classQueryUseCase = mock(ClassQueryUseCase.class);
        slotManagementUseCase = mock(SlotManagementUseCase.class);
        slotQueryUseCase = mock(SlotQueryUseCase.class);
        bookingCalendarUseCase = mock(BookingCalendarUseCase.class);
        imageMediaUseCase = mock(ImageMediaUseCase.class);

        ProductQueryUseCase.ProductView product = RestDocsFixtures.productWithInventory();
        ProductQueryUseCase.ProductView inactiveProduct =
                RestDocsFixtures.productWithInventory(ProductStatus.INACTIVE);
        InventoryAdjustment inventoryAdjustment = inventoryAdjustment();
        BookingClass bookingClass = RestDocsFixtures.bookingClass();
        Slot slot = RestDocsFixtures.slot();
        WorkshopProfile workshop = workshopProfile();

        when(adminAuthUseCase.login("admin", "admin123456"))
                .thenReturn(LoginResult.authenticated("admin-session-token"));
        when(adminAuthUseCase.verifyMfa("mfa-challenge-token", "123456"))
                .thenReturn(LoginResult.authenticated("admin-session-token"));
        when(adminMfaUseCase.getStatus(ADMIN_USER_ID))
                .thenReturn(new MfaStatus(false, false, 0));
        when(adminMfaUseCase.beginEnrollment(ADMIN_USER_ID))
                .thenReturn(new MfaEnrollment(
                        "JBSWY3DPEHPK3PXP",
                        "otpauth://totp/%ED%95%B4%ED%94%BC%EA%B0%A4%EB%9F%AC%EB%A6%AC:admin"
                                + "?secret=JBSWY3DPEHPK3PXP"));
        when(adminMfaUseCase.confirmEnrollment(ADMIN_USER_ID, "123456"))
                .thenReturn(new RecoveryCodes(List.of(
                        "aaaa-bbbb-cccc-0001",
                        "aaaa-bbbb-cccc-0002")));
        when(adminSetupUseCase.isAvailable()).thenReturn(true);
        when(productAdminUseCase.register(any()))
                .thenReturn(new ProductAdminUseCase.ProductResult(
                        product.product(), product.quantity(), product.available(), product.options()));
        when(productQueryUseCase.listAllProducts()).thenReturn(List.of(product));
        when(productAdminUseCase.update(eq(1L), any()))
                .thenReturn(new ProductAdminUseCase.ProductResult(
                        product.product(), product.quantity(), product.available(), product.options()));
        when(productAdminUseCase.changeStatus(1L, ProductStatus.INACTIVE))
                .thenReturn(new ProductAdminUseCase.ProductResult(
                        inactiveProduct.product(), inactiveProduct.quantity(),
                        inactiveProduct.available(), inactiveProduct.options()));
        when(productAdminUseCase.adjustInventory(any())).thenReturn(inventoryAdjustment);
        when(productAdminUseCase.listRecentInventoryAdjustments(1L))
                .thenReturn(List.of(inventoryAdjustment));
        MappingResult smartStoreMapping = new MappingResult(
                1L,
                17L,
                123456789L,
                true,
                List.of(),
                SmartStoreStockSyncStatus.PENDING,
                0,
                null,
                null);
        when(smartStoreInventoryUseCase.saveMapping(eq(1L), any(), any())).thenReturn(smartStoreMapping);
        when(smartStoreInventoryUseCase.getMapping(1L)).thenReturn(Optional.of(smartStoreMapping));
        when(smartStoreInventoryUseCase.listMappingHistory(1L)).thenReturn(List.of(new MappingHistoryResult(
                21L,
                SmartStoreInventoryMappingAction.ORIGIN_CHANGED,
                111111111L,
                123456789L,
                true,
                true,
                "조합 31 → 옵션 81",
                "조합 31 → 옵션 91",
                16L,
                17L,
                true,
                ADMIN_USER_ID,
                "admin",
                LocalDateTime.of(2026, 9, 2, 14, 30))));
        when(smartStoreInventoryUseCase.retry(1L)).thenReturn(smartStoreMapping);
        when(smartStoreInventoryUseCase.previewProduct(1L)).thenReturn(new ProductPreviewResult(
                1L, "preview-v1", 123456789L, 35000L, 33000L,
                "SALE", "SALE", true, List.of()));
        when(smartStoreInventoryUseCase.listChannelProducts(1, 100))
                .thenReturn(new CatalogPageResult(
                        List.of(new CatalogProductResult(
                                123456789L, 987654321L, "각인 카드지갑", "SALE", 33000L, 7,
                                "https://images.example.com/wallet.jpg")),
                        1, 100, 1, 1));
        when(smartStoreInventoryUseCase.getChannelProduct(123456789L))
                .thenReturn(new ChannelProductResult(
                        123456789L, 33000L, "SALE",
                        List.of(new ChannelOptionResult(
                                11L, "브라운 / 금박", 3, 1000L, true))));
        ChannelOrderResult smartStoreOrder = new ChannelOrderResult(
                "2026082912345678", "2026082911111111", 123456789L, 90001L,
                1L, 31L, "각인 카드지갑", "색상: 브라운", "RETURNED",
                "RETURN", "RETURN_DONE", 2, 0, 2,
                SmartStoreOrderAttentionReason.RETURN_REVIEW,
                LocalDateTime.of(2026, 8, 29, 11, 58),
                LocalDateTime.of(2026, 8, 29, 12, 0), 2, "R2:0", "resolution-v1");
        when(smartStoreChannelOrderUseCase.list(false, null, null, 50))
                .thenReturn(new CursorPage<>(List.of(smartStoreOrder), "next-order-cursor", true));
        when(smartStoreChannelOrderUseCase.list(
                true, SmartStoreOrderAttentionReason.STOCK_SHORTAGE, "order-cursor", 25))
                .thenReturn(new CursorPage<>(List.of(smartStoreOrder), null, false));
        when(smartStoreChannelOrderUseCase.listReturnDeliveryCompanies())
                .thenReturn(List.of(new ReturnDeliveryCompanyResult(1001L, "CJ대한통운", "PRIMARY")));
        when(smartStoreChannelOrderUseCase.retryInventory("2026082912345678"))
                .thenReturn(smartStoreOrder);
        when(smartStoreChannelOrderUseCase.resolveReturn("2026082912345678", true, "R2:0"))
                .thenReturn(new ChannelOrderResult(
                        "2026082912345678", "2026082911111111", 123456789L, 90001L,
                        1L, 31L, "각인 카드지갑", "색상: 브라운", "RETURNED",
                        "RETURN", "RETURN_DONE", 2, 0, 0, null,
                        LocalDateTime.of(2026, 8, 29, 11, 58),
                        LocalDateTime.of(2026, 8, 29, 12, 0), 0, "R2:2", "resolution-v2"));
        when(smartStoreChannelOrderUseCase.resolveInventory(any(), any()))
                .thenReturn(smartStoreOrder);
        when(smartStoreChannelOrderUseCase.listActionHistory("2026082912345678"))
                .thenReturn(List.of(new ActionHistoryResult(
                        71L,
                        "2026082912345678",
                        SmartStoreOrderAction.INVENTORY_RESOLVED,
                        SmartStoreOrderActionStatus.SUCCEEDED,
                        "상품 1, 옵션 조합 31, 재고 반영 방법 APPLY_REMAINING, 목표 적용 2개, 사유: 매핑 확인",
                        null,
                        null,
                        ADMIN_USER_ID,
                        "admin",
                        LocalDateTime.of(2026, 9, 2, 14, 30),
                        LocalDateTime.of(2026, 9, 2, 14, 30),
                        null, null, null, null, null)));
        ActionHistoryResult unresolvedAction = new ActionHistoryResult(
                72L,
                "2026082912345678",
                SmartStoreOrderAction.ORDER_DISPATCHED,
                SmartStoreOrderActionStatus.RESULT_UNKNOWN,
                "배송 방법 DELIVERY, 택배사 CJGLS, 운송장 1234567890, 발송일 2026-09-02T14:30",
                "RESULT_UNKNOWN",
                "네이버 응답에서 처리 결과를 확인할 수 없습니다.",
                ADMIN_USER_ID,
                "admin",
                LocalDateTime.of(2026, 9, 2, 14, 30),
                LocalDateTime.of(2026, 9, 2, 14, 30),
                null, null, null, null, null);
        when(smartStoreChannelOrderUseCase.listUnresolvedActions("action-cursor", 20))
                .thenReturn(new CursorPage<>(List.of(unresolvedAction), null, false));
        ActionHistoryResult reconciledAction = new ActionHistoryResult(
                unresolvedAction.id(), unresolvedAction.productOrderId(), unresolvedAction.action(),
                unresolvedAction.status(), unresolvedAction.requestSummary(), unresolvedAction.resultCode(),
                unresolvedAction.resultMessage(), unresolvedAction.changedByAdminId(),
                unresolvedAction.changedBy(), unresolvedAction.requestedAt(), unresolvedAction.completedAt(),
                SmartStoreOrderReconciliationOutcome.APPLIED,
                "네이버 주문 상세에서 발송 완료와 운송장 번호를 확인",
                ADMIN_USER_ID,
                "admin",
                LocalDateTime.of(2026, 9, 2, 14, 40));
        when(smartStoreChannelOrderUseCase.reconcileAction(eq(72L), any(), any()))
                .thenReturn(reconciledAction);
        when(smartStoreChannelOrderUseCase.currentStatus("2026082912345678"))
                .thenReturn(new CurrentOrderStatusResult(
                        "2026082912345678", "DELIVERING", "OK", null, null, 2,
                        LocalDateTime.of(2026, 9, 3, 18, 0), "DELIVERY",
                        "CJ대한통운", "1234567890", null));
        when(smartStoreChannelOrderUseCase.detail("2026082912345678"))
                .thenReturn(new ChannelOrderDetailResult(
                        smartStoreOrder,
                        new DeliveryInfo(
                                "홍길동", "01012345678", "04524",
                                "서울 중구 세종대로 110", "2층", "문 앞에 놓아주세요"),
                        "OK", LocalDateTime.of(2026, 8, 30, 18, 0), "DELIVERY",
                        "CJ대한통운", "1234567890", 35000L, 70000L, 1000L,
                        2000L, 300L, 66700L,
                        new ClaimDetail(
                                "claim-1", "RETURN", "RETURN_DONE", "PRODUCT_UNSATISFIED",
                                "색상이 달라요", 1, LocalDateTime.of(2026, 8, 29, 12, 30),
                                "COLLECT_DONE", "CJ대한통운", "9876543210", 3000L,
                                null, List.of())));
        when(smartStoreInquiryUseCase.list(true, 100)).thenReturn(List.of(new InquiryResult(
                456L, 123L, "가죽 지갑", "cust***", "각인 가능한가요?", null,
                false, LocalDateTime.of(2026, 8, 29, 10, 0))));
        when(smartStoreInquiryUseCase.getProductInquiryAnswerTemplate())
                .thenReturn(new AnswerTemplateResult(
                        "PRODUCT", "상품 문의 기본 답변", "문의해 주셔서 감사합니다."));
        when(smartStoreInquiryUseCase.listCustomerInquiries(true, 100))
                .thenReturn(List.of(new CustomerInquiryResult(
                        789L, null, "DELIVERY", "배송 문의", "언제 도착하나요?", null,
                        false, "order-1", "123", "po-1", "가죽 지갑", "브라운",
                        "cust***", "홍*동", LocalDateTime.of(2026, 8, 29, 11, 0), null)));
        when(workshopProfileUseCase.get()).thenReturn(workshop);
        when(workshopProfileUseCase.update(any())).thenReturn(workshop);
        when(classManagementUseCase.createClass(any())).thenReturn(bookingClass);
        when(classQueryUseCase.listAll()).thenReturn(List.of(bookingClass));
        when(slotQueryUseCase.listByClass(1L)).thenReturn(List.of(slot));
        when(slotManagementUseCase.deactivateSlot(42L)).thenReturn(slot);
        when(slotManagementUseCase.activateSlot(42L)).thenReturn(slot);
        BookingCalendarSettings calendarSettings = new BookingCalendarSettings(
                LocalTime.of(10, 0), LocalTime.of(19, 0), 30, true);
        BookingTimeBlock timeBlock = mock(BookingTimeBlock.class);
        when(timeBlock.getId()).thenReturn(7L);
        when(timeBlock.getDate()).thenReturn(LocalDate.of(2026, 5, 7));
        when(timeBlock.getStartTime()).thenReturn(LocalTime.of(12, 0));
        when(timeBlock.getEndTime()).thenReturn(LocalTime.of(13, 0));
        when(timeBlock.getReason()).thenReturn("점심시간");
        when(bookingCalendarUseCase.getCalendar(any(), any())).thenReturn(new CalendarView(
                calendarSettings,
                List.of(new CalendarDay(
                        LocalDate.of(2026, 5, 7),
                        false,
                        BookingDayAvailability.OPEN,
                        DayOverrideMode.DEFAULT,
                        null,
                        List.of(timeBlock)))));
        when(bookingCalendarUseCase.updateSettings(any())).thenReturn(calendarSettings);
        when(bookingCalendarUseCase.createTimeBlock(any())).thenReturn(timeBlock);

        var restockDemand = mock(RestockDemandUseCase.class);
        when(restockDemand.list(null, 0, 20)).thenReturn(OffsetPage.of(List.of(
                new RestockDemandUseCase.Demand(42L, "레진 키링", 50L, "색상: 파랑", 3L)), 0, 20, 1));
        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
                new AdminRestockDemandController(restockDemand),
                new AdminLoginController(adminAuthUseCase, new AdminBearerTokenResolver()),
                new AdminCredentialController(adminCredentialUseCase),
                new AdminMfaController(adminMfaUseCase),
                new AdminSetupController(new AdminSetupProperties("setup-token"), adminSetupUseCase),
                new AdminProductController(
                        productAdminUseCase, productQueryUseCase, smartStoreInventoryUseCase),
                new AdminSmartStoreOrderController(smartStoreChannelOrderUseCase),
                new AdminSmartStoreInquiryController(smartStoreInquiryUseCase),
                new AdminMediaController(imageMediaUseCase),
                new AdminWorkshopProfileController(workshopProfileUseCase),
                new AdminClassController(classManagementUseCase, classQueryUseCase),
                new AdminSlotController(
                        slotManagementUseCase, slotQueryUseCase, bookingCalendarUseCase));
    }

    @Test
    @DisplayName("관리자 재입고 대기 현황은 상품·옵션별 인원만 제공한다")
    void list_restock_demand() throws Exception {
        mockMvc.perform(get("/api/v1/admin/restock-demand").with(adminUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].waitingCount").value(3))
                .andExpect(jsonPath("$.content[0].phone").doesNotExist());
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
    @DisplayName("관리자 MFA 로그인 확인 API를 문서화한다")
    void admin_verify_mfa() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/mfa/verify")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "challengeToken": "mfa-challenge-token",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 MFA 상태 API를 문서화한다")
    void admin_mfa_status() throws Exception {
        mockMvc.perform(get("/api/v1/admin/auth/mfa")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 MFA 등록 시작 API를 문서화한다")
    void admin_begin_mfa_enrollment() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/mfa/enrollment")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 MFA 등록 확인 API를 문서화한다")
    void admin_confirm_mfa_enrollment() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/mfa/enrollment/confirm")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"code\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 MFA 해제 API를 문서화한다")
    void admin_disable_mfa() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/auth/mfa")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "admin123456",
                                  "code": "123456"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("복구 코드 로그인 세션의 관리자 MFA 복구 API를 문서화한다")
    void admin_recover_mfa() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/mfa/recovery")
                        .with(adminUser(AdminAuthenticationMethod.RECOVERY_CODE))
                        .header("Authorization", "Bearer recovery-admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"currentPassword\":\"admin123456\"}"))
                .andExpect(status().isNoContent());
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
                                  "quantity": 12,
                                  "optionGroups": [],
                                  "variants": []
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
    @DisplayName("관리자는 공개 참조 전 이미지도 인증된 미리보기 경로로 조회한다")
    void admin_get_image_preview() throws Exception {
        String fileName = "11111111-1111-4111-8111-111111111111.jpg";
        when(imageMediaUseCase.get(fileName)).thenReturn(
                new ImageMediaUseCase.ImageContent(new byte[] {1, 2, 3}, "image/jpeg"));

        mockMvc.perform(get("/api/v1/admin/media/images/{fileName}", fileName)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    @DisplayName("관리자 공방과 사업자 정보 수정 API를 문서화한다")
    void admin_update_workshop_profile() throws Exception {
        mockMvc.perform(put("/api/v1/admin/workshop")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "name": "해피갤러리",
                                  "phone": "010-9635-5608",
                                  "postalCode": null,
                                  "addressLine1": "충북 충주시 계명대로 161",
                                  "addressLine2": "1층",
                                  "businessHours": null,
                                  "mapUrl": "https://m.place.naver.com/place/21668321",
                                  "parkingInfo": null,
                                  "businessRegistrationNumber": "303-11-87052",
                                  "representativeName": "홍지현",
                                  "email": "ssi1972@naver.com",
                                  "mailOrderRegistrationNumber": "2011-충북 충주-127",
                                  "introduction": "해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, 양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.",
                                  "kakaoTalkId": "ssim1972",
                                  "naverTalkUrl": "https://talk.naver.com/w4xufy",
                                  "naverBlogUrl": "https://blog.naver.com/ssim1972",
                                  "instagramUrl": "https://www.instagram.com/happygallery_by/",
                                  "smartStoreUrl": "https://smartstore.naver.com/happygallery"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessRegistrationNumber").value("303-11-87052"))
                .andExpect(jsonPath("$.representativeName").value("홍지현"))
                .andExpect(jsonPath("$.email").value("ssi1972@naver.com"))
                .andExpect(jsonPath("$.mailOrderRegistrationNumber").value("2011-충북 충주-127"))
                .andExpect(jsonPath("$.naverTalkUrl").value("https://talk.naver.com/w4xufy"))
                .andExpect(jsonPath("$.naverBlogUrl").value("https://blog.naver.com/ssim1972"))
                .andExpect(jsonPath("$.instagramUrl")
                        .value("https://www.instagram.com/happygallery_by/"))
                .andExpect(jsonPath("$.smartStoreUrl")
                        .value("https://smartstore.naver.com/happygallery"));
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
                                  "imageUrl": "https://images.example.com/candle.jpg",
                                  "optionGroups": [],
                                  "variants": []
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문제작 상품 수정은 기존 조합의 현재 재고를 반환한다")
    void admin_update_product_options() throws Exception {
        Product product = new Product("주문제작 키링", ProductType.MADE_TO_ORDER, null,
                10000L, null, null, "가죽 키링", null, 3);
        ReflectionTestUtils.setField(product, "id", 2L);
        when(productAdminUseCase.update(eq(2L), any())).thenReturn(new ProductAdminUseCase.ProductResult(
                product, 4, true, new ProductOptions(List.of(),
                List.of(new ProductOptions.Variant(20L, 2000L, 4, true, List.of())))));

        mockMvc.perform(patch("/api/v1/admin/products/{id}", 2L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "주문제작 키링",
                                  "price": 10000,
                                  "specification": "가죽 키링",
                                  "productionLeadDays": 3,
                                  "optionGroups": [],
                                  "variants": [{"selections": [], "priceAdjustment": 2000, "quantity": 5, "active": true}]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(4))
                .andExpect(jsonPath("$.variants[0].id").value(20))
                .andExpect(jsonPath("$.variants[0].quantity").value(4));
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
    @DisplayName("관리자 스마트스토어 재고 연동 설정 저장 API를 문서화한다")
    void admin_save_smartstore_inventory_mapping() throws Exception {
        mockMvc.perform(put("/api/v1/admin/products/{id}/smartstore-inventory", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "originProductNo": 123456789,
                                  "enabled": true,
                                  "expectedMappingVersion": 17,
                                  "previousOriginConfirmed": false,
                                  "variants": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappingVersion").value(17))
                .andExpect(jsonPath("$.syncStatus").value("PENDING"));
        verify(smartStoreInventoryUseCase).saveMapping(
                eq(1L), any(), argThat(actor ->
                        actor.adminUserId().equals(ADMIN_USER_ID) && actor.name().equals("admin")));
    }

    @Test
    @DisplayName("관리자 스마트스토어 재고 연동 설정 조회 API를 문서화한다")
    void admin_get_smartstore_inventory_mapping() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/{id}/smartstore-inventory", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappingVersion").value(17))
                .andExpect(jsonPath("$.originProductNo").value(123456789));
    }

    @Test
    @DisplayName("관리자 스마트스토어 재고 연동 변경 이력 API를 문서화한다")
    void admin_list_smartstore_inventory_mapping_history() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/{id}/smartstore-inventory/history", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("ORIGIN_CHANGED"))
                .andExpect(jsonPath("$[0].previousOriginProductNo").value(111111111))
                .andExpect(jsonPath("$[0].nextOriginProductNo").value(123456789))
                .andExpect(jsonPath("$[0].changedBy").value("admin"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 재고 동기화 재시도 API를 문서화한다")
    void admin_retry_smartstore_inventory_sync() throws Exception {
        mockMvc.perform(post("/api/v1/admin/products/{id}/smartstore-inventory/retry", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("PENDING"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 상품 차이 조회와 반영 API를 문서화한다")
    void admin_preview_and_apply_smartstore_product() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/{id}/smartstore-product-preview", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewVersion").value("preview-v1"))
                .andExpect(jsonPath("$.different").value(true));
        mockMvc.perform(post("/api/v1/admin/products/{id}/smartstore-product-sync", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"previewVersion\":\"preview-v1\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/products/{id}/smartstore-product-sync", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"productVersion\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자 스마트스토어 상품 목록과 옵션 조회 API를 문서화한다")
    void admin_list_and_get_smartstore_products() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/smartstore-catalog")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[0].originProductNo").value(123456789));
        mockMvc.perform(get("/api/v1/admin/products/smartstore-catalog/{originProductNo}",
                        123456789L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].name").value("브라운 / 금박"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 재고 연동 해제 API를 문서화한다")
    void admin_delete_smartstore_inventory_mapping() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/products/{id}/smartstore-inventory", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .queryParam("expectedMappingVersion", "17")
                        .queryParam("previousOriginConfirmed", "true"))
                .andExpect(status().isNoContent());
        verify(smartStoreInventoryUseCase).deleteMapping(
                eq(1L), any(), argThat(actor ->
                        actor.adminUserId().equals(ADMIN_USER_ID) && actor.name().equals("admin")));
    }

    @Test
    @DisplayName("관리자 스마트스토어 채널 주문 목록 API를 문서화한다")
    void admin_list_smartstore_channel_orders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].attentionReason").value("RETURN_REVIEW"))
                .andExpect(jsonPath("$.nextCursor").value("next-order-cursor"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("관리자 스마트스토어 확인 대상 주문의 사유 필터와 커서 API를 문서화한다")
    void admin_list_smartstore_attention_orders_with_reason_and_cursor() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .queryParam("attentionOnly", "true")
                        .queryParam("attentionReason", "STOCK_SHORTAGE")
                        .queryParam("cursor", "order-cursor")
                        .queryParam("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productOrderId").value("2026082912345678"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문 재고 반영 방법 지정 API를 문서화한다")
    void admin_resolve_smartstore_order_inventory() throws Exception {
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/{productOrderId}/inventory-resolution",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "productId":1,
                                  "productVariantId":31,
                                  "action":"APPLY_REMAINING",
                                  "reason":"스마트스토어 옵션과 내부 옵션 조합을 확인",
                                  "resolutionVersion":"resolution-v1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryResolutionVersion").value("resolution-v1"));

        verify(smartStoreChannelOrderUseCase).resolveInventory(
                argThat(command -> command.productOrderId().equals("2026082912345678")
                        && command.productId().equals(1L)
                        && command.productVariantId().equals(31L)
                        && command.action() == SmartStoreInventoryResolutionAction.APPLY_REMAINING
                        && command.reason().equals("스마트스토어 옵션과 내부 옵션 조합을 확인")
                        && command.expectedResolutionVersion().equals("resolution-v1")),
                argThat(actor -> actor.adminUserId().equals(ADMIN_USER_ID)
                        && actor.name().equals("admin")));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문 처리 이력 조회 API를 문서화한다")
    void admin_list_smartstore_order_action_history() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders/{productOrderId}/actions",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("INVENTORY_RESOLVED"))
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].changedBy").value("admin"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 미확정 주문 처리 목록 API를 문서화한다")
    void admin_list_unresolved_smartstore_order_actions() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders/actions/unresolved")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .queryParam("cursor", "action-cursor")
                        .queryParam("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productOrderId").value("2026082912345678"))
                .andExpect(jsonPath("$.content[0].status").value("RESULT_UNKNOWN"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문 처리 대사 API를 문서화한다")
    void admin_reconcile_smartstore_order_action() throws Exception {
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/actions/{historyId}/reconciliation", 72L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "outcome":"APPLIED",
                                  "note":"네이버 주문 상세에서 발송 완료와 운송장 번호를 확인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationOutcome").value("APPLIED"))
                .andExpect(jsonPath("$.reconciledBy").value("admin"));

        verify(smartStoreChannelOrderUseCase).reconcileAction(
                eq(72L),
                argThat(command -> command.outcome() == SmartStoreOrderReconciliationOutcome.APPLIED
                        && command.note().equals("네이버 주문 상세에서 발송 완료와 운송장 번호를 확인")),
                argThat(actor -> actor.adminUserId().equals(ADMIN_USER_ID)
                        && actor.name().equals("admin")));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문의 네이버 현재 상태 조회 API를 문서화한다")
    void admin_get_current_smartstore_order_status() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders/{productOrderId}/current-status",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productOrderStatus").value("DELIVERING"))
                .andExpect(jsonPath("$.deliveryCompany").value("CJ대한통운"))
                .andExpect(jsonPath("$.trackingNumber").value("1234567890"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문 재고 반영 재시도 API를 문서화한다")
    void admin_retry_smartstore_channel_order_inventory() throws Exception {
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/{productOrderId}/inventory/retry",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 스마트스토어 반품 재고 처리 API를 문서화한다")
    void admin_resolve_smartstore_channel_order_return() throws Exception {
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/{productOrderId}/return-resolution",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"restoreStock\":true,\"reviewVersion\":\"R2:0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingReturnQuantity").value(0))
                .andExpect(jsonPath("$.returnReviewVersion").value("R2:2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"restoreStock\":true}", "{\"restoreStock\":false,\"reviewVersion\":\" \"}"})
    @DisplayName("반품 검수 확인값이 없거나 공백이면 요청을 거절한다")
    void admin_resolve_smartstore_return_requires_review_version(String request) throws Exception {
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/{productOrderId}/return-resolution",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("반품 검수 대상이 바뀌면 충돌 응답을 반환한다")
    void admin_resolve_smartstore_return_rejects_changed_review_version() throws Exception {
        when(smartStoreChannelOrderUseCase.resolveReturn("2026082912345678", true, "R1:0"))
                .thenThrow(new HappyGalleryException(ErrorCode.CONFLICT,
                        "반품 검수 대상이 변경되었습니다. 최신 수량을 다시 확인해 주세요."));
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/{productOrderId}/return-resolution",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"restoreStock\":true,\"reviewVersion\":\"R1:0\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문 상세 API를 문서화한다")
    void admin_get_smartstore_channel_order() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders/{productOrderId}",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryInfo.recipientName").value("홍길동"))
                .andExpect(jsonPath("$.expectedSettlementAmount").value(66700));
    }

    @Test
    @DisplayName("관리자 스마트스토어 주문 발송 API를 문서화한다")
    void admin_dispatch_smartstore_channel_order() throws Exception {
        mockMvc.perform(post("/api/v1/admin/smartstore-orders/{productOrderId}/dispatch",
                        "2026082912345678")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryMethod":"DELIVERY",
                                  "deliveryCompanyCode":"CJGLS",
                                  "trackingNumber":"1234567890",
                                  "dispatchDate":"2026-08-29T15:00:00"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 스마트스토어 상품 문의 조회와 답변 API를 문서화한다")
    void admin_manage_smartstore_inquiry() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-inquiries")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionId").value(456));
        when(smartStoreInquiryUseCase.listPage(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), true, 1, 50))
                .thenReturn(new OffsetPage<>(List.of(new InquiryResult(
                        456L, 123L, "가죽 지갑", "cust***", "각인 가능한가요?", null,
                        false, LocalDateTime.of(2026, 7, 29, 10, 0))), 1, 50, 101, 3));
        mockMvc.perform(get("/api/v1/admin/smartstore-inquiries/page")
                        .with(adminUser()).header("Authorization", "Bearer admin-session-token")
                        .param("from", "2026-07-01").param("to", "2026-07-31").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].questionId").value(456))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalCount").value(101));
        mockMvc.perform(put("/api/v1/admin/smartstore-inquiries/{questionId}/answer", 456L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"원하시는 문구로 가능합니다.\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 스마트스토어 상품 문의 답변 템플릿 API를 문서화한다")
    void admin_get_smartstore_inquiry_answer_template() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-inquiries/template")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("상품 문의 기본 답변"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 고객 문의 조회와 답변 API를 문서화한다")
    void admin_manage_smartstore_customer_inquiry() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-inquiries/customers")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inquiryNo").value(789));
        when(smartStoreInquiryUseCase.listCustomerPage(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), false, 0, 50))
                .thenReturn(new OffsetPage<>(List.of(), 0, 50, 0, 0));
        mockMvc.perform(get("/api/v1/admin/smartstore-inquiries/customers/page")
                        .with(adminUser()).header("Authorization", "Bearer admin-session-token")
                        .param("from", "2026-07-01").param("to", "2026-07-31").param("unansweredOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalPages").value(0));
        mockMvc.perform(put(
                        "/api/v1/admin/smartstore-inquiries/customers/{inquiryNo}/answer", 789L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"오늘 출고 예정입니다.\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 스마트스토어 고객 문의 답변 수정 API를 문서화한다")
    void admin_update_smartstore_customer_inquiry_answer() throws Exception {
        when(smartStoreInquiryUseCase.listCustomerInquiries(false, 100))
                .thenReturn(List.of(new CustomerInquiryResult(
                        789L, 456L, "DELIVERY", "배송 문의", "언제 도착하나요?", "오늘 출고 예정입니다.",
                        true, "order-1", "123", "po-1", "가죽 지갑", "브라운",
                        "cust***", "홍*동", LocalDateTime.of(2026, 8, 29, 11, 0), null)));
        mockMvc.perform(get("/api/v1/admin/smartstore-inquiries/customers")
                        .queryParam("unansweredOnly", "false")
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].answerContentId").value(456));
        mockMvc.perform(put(
                        "/api/v1/admin/smartstore-inquiries/customers/{inquiryNo}/answer/{answerContentId}",
                        789L, 456L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"내일 출고 예정입니다.\"}"))
                .andExpect(status().isNoContent());
        verify(smartStoreInquiryUseCase).updateCustomerInquiryAnswer(789L, 456L, "내일 출고 예정입니다.");
    }

    @Test
    @DisplayName("관리자 스마트스토어 반품 택배사 계약 조회 API를 문서화한다")
    void admin_list_smartstore_return_delivery_companies() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders/return-delivery-companies")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1001))
                .andExpect(jsonPath("$[0].priorityType").value("PRIMARY"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 교환·판매자 취소 처리 API를 문서화한다")
    void admin_manage_smartstore_exchange_and_seller_cancel() throws Exception {
        String path = "/api/v1/admin/smartstore-orders/{productOrderId}/claims";
        mockMvc.perform(post(path + "/exchange/collect/complete", "2026082912345678")
                        .with(adminUser()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(path + "/exchange/reject", "2026082912345678")
                        .with(adminUser()).contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"교환 대상 상품이 아닙니다.\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(path + "/exchange/hold", "2026082912345678")
                        .with(adminUser()).contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "holdbackClassType":"EXCHANGE_DELIVERYFEE",
                                  "detailedReason":"교환 배송비 입금 대기",
                                  "extraExchangeFeeAmount":3000
                                }
                                """))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(path + "/exchange/hold/release", "2026082912345678")
                        .with(adminUser()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(path + "/cancel/request", "2026082912345678")
                        .with(adminUser()).contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "reason":"SOLD_OUT",
                                  "detailedReason":"부자재 품절",
                                  "quantity":1
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 스마트스토어 반품 보류와 판매자 반품 요청 API를 문서화한다")
    void admin_manage_smartstore_return() throws Exception {
        String path = "/api/v1/admin/smartstore-orders/{productOrderId}/claims/return";
        mockMvc.perform(post(path + "/hold", "2026082912345678")
                        .with(adminUser()).contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "holdbackClassType":"RETURN_DELIVERYFEE",
                                  "detailedReason":"반품 배송비 입금 대기",
                                  "extraReturnFeeAmount":3000
                                }
                                """))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(path + "/hold/release", "2026082912345678")
                        .with(adminUser()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(path + "/request", "2026082912345678")
                        .with(adminUser()).contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "returnReason":"PRODUCT_UNSATISFIED",
                                  "collectDeliveryMethod":"RETURN_DESIGNATED",
                                  "collectDeliveryCompany":"CJGLS",
                                  "collectTrackingNumber":"1234567890",
                                  "returnQuantity":1
                                }
                                """))
                .andExpect(status().isNoContent());
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
                                  "capacity": 6,
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
    @DisplayName("관리자 예약 캘린더 조회 API를 문서화한다")
    void admin_get_booking_calendar() throws Exception {
        mockMvc.perform(get("/api/v1/admin/slots/calendar")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .param("dateFrom", "2026-05-01")
                        .param("dateTo", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.slotIntervalMin").value(30));
    }

    @Test
    @DisplayName("관리자 기본 예약 운영시간 수정 API를 문서화한다")
    void admin_update_booking_calendar_settings() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/slots/calendar/settings")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "openTime": "10:00",
                                  "closeTime": "19:00",
                                  "slotIntervalMin": 30,
                                  "blockPublicHolidays": true
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 날짜별 예약 상태 수정 API를 문서화한다")
    void admin_update_booking_calendar_day() throws Exception {
        mockMvc.perform(put("/api/v1/admin/slots/calendar/days/{date}", "2026-05-07")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"mode\":\"CLOSED\",\"reason\":\"외부 일정\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 예약 시간 차단 등록 API를 문서화한다")
    void admin_create_booking_time_block() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots/calendar/time-blocks")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-05-07",
                                  "startTime": "12:00",
                                  "endTime": "13:00",
                                  "reason": "점심시간"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @DisplayName("관리자 예약 시간 차단 해제 API를 문서화한다")
    void admin_delete_booking_time_block() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/slots/calendar/time-blocks/{id}", 7L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isNoContent());
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

    private static WorkshopProfile workshopProfile() {
        WorkshopProfile profile = new WorkshopProfile("해피갤러리");
        profile.update(
                "해피갤러리", "010-9635-5608", null,
                "충북 충주시 계명대로 161", "1층", null,
                "https://m.place.naver.com/place/21668321", null,
                "303-11-87052", "홍지현", "ssi1972@naver.com", "2011-충북 충주-127",
                "해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, "
                        + "양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.",
                "ssim1972",
                "https://talk.naver.com/w4xufy",
                "https://blog.naver.com/ssim1972",
                "https://www.instagram.com/happygallery_by/",
                "https://smartstore.naver.com/happygallery",
                LocalDateTime.of(2026, 5, 1, 21, 0));
        return profile;
    }
}
