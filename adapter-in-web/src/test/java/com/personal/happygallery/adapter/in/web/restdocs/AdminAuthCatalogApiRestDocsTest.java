package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminClassController;
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
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ChannelOrderDetailResult;
import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.DeliveryInfo;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.InquiryResult;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.MappingResult;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase.VariantMapping;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import com.personal.happygallery.domain.booking.BookingTimeBlock;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.SmartStoreStockSyncStatus;
import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
                123456789L,
                true,
                List.of(),
                SmartStoreStockSyncStatus.PENDING,
                0,
                null,
                null);
        when(smartStoreInventoryUseCase.saveMapping(eq(1L), any())).thenReturn(smartStoreMapping);
        when(smartStoreInventoryUseCase.getMapping(1L)).thenReturn(Optional.of(smartStoreMapping));
        when(smartStoreInventoryUseCase.retry(1L)).thenReturn(smartStoreMapping);
        ChannelOrderResult smartStoreOrder = new ChannelOrderResult(
                "2026082912345678", "2026082911111111", 123456789L, 90001L,
                1L, 31L, "각인 카드지갑", "색상: 브라운", "RETURNED",
                "RETURN", "RETURN_DONE", 2, 0, 2,
                SmartStoreOrderAttentionReason.RETURN_REVIEW,
                LocalDateTime.of(2026, 8, 29, 11, 58),
                LocalDateTime.of(2026, 8, 29, 12, 0));
        when(smartStoreChannelOrderUseCase.list(false, 100))
                .thenReturn(List.of(smartStoreOrder));
        when(smartStoreChannelOrderUseCase.retryInventory("2026082912345678"))
                .thenReturn(smartStoreOrder);
        when(smartStoreChannelOrderUseCase.resolveReturn("2026082912345678", true))
                .thenReturn(smartStoreOrder);
        when(smartStoreChannelOrderUseCase.detail("2026082912345678"))
                .thenReturn(new ChannelOrderDetailResult(
                        smartStoreOrder,
                        new DeliveryInfo(
                                "홍길동", "01012345678", "04524",
                                "서울 중구 세종대로 110", "2층", "문 앞에 놓아주세요"),
                        "OK", LocalDateTime.of(2026, 8, 30, 18, 0), "DELIVERY",
                        "CJ대한통운", "1234567890", 35000L, 70000L, 1000L,
                        2000L, 300L, 66700L));
        when(smartStoreInquiryUseCase.list(true, 100)).thenReturn(List.of(new InquiryResult(
                456L, 123L, "가죽 지갑", "cust***", "각인 가능한가요?", null,
                false, LocalDateTime.of(2026, 8, 29, 10, 0))));
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

        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
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
                                  "variants": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatus").value("PENDING"));
    }

    @Test
    @DisplayName("관리자 스마트스토어 재고 연동 설정 조회 API를 문서화한다")
    void admin_get_smartstore_inventory_mapping() throws Exception {
        mockMvc.perform(get("/api/v1/admin/products/{id}/smartstore-inventory", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originProductNo").value(123456789));
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
    @DisplayName("관리자 스마트스토어 재고 연동 해제 API를 문서화한다")
    void admin_delete_smartstore_inventory_mapping() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/products/{id}/smartstore-inventory", 1L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 스마트스토어 채널 주문 목록 API를 문서화한다")
    void admin_list_smartstore_channel_orders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/smartstore-orders")
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attentionReason").value("RETURN_REVIEW"));
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
                        .content("{\"restoreStock\":true}"))
                .andExpect(status().isOk());
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
        mockMvc.perform(put("/api/v1/admin/smartstore-inquiries/{questionId}/answer", 456L)
                        .with(adminUser())
                        .header("Authorization", "Bearer admin-session-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"content\":\"원하시는 문구로 가능합니다.\"}"))
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
