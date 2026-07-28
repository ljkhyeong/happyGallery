package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminClassController;
import com.personal.happygallery.adapter.in.web.admin.AdminCredentialController;
import com.personal.happygallery.adapter.in.web.admin.AdminLoginController;
import com.personal.happygallery.adapter.in.web.admin.AdminMfaController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductController;
import com.personal.happygallery.adapter.in.web.admin.AdminSetupController;
import com.personal.happygallery.adapter.in.web.admin.AdminSlotController;
import com.personal.happygallery.adapter.in.web.admin.AdminWorkshopProfileController;
import com.personal.happygallery.adapter.in.web.config.properties.AdminSetupProperties;
import com.personal.happygallery.adapter.in.web.security.admin.AdminBearerTokenResolver;
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
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotItem;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotResult;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotStatus;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.product.InventoryAdjustment;
import com.personal.happygallery.domain.product.InventoryAdjustmentType;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private WorkshopProfileUseCase workshopProfileUseCase;
    private ClassManagementUseCase classManagementUseCase;
    private ClassQueryUseCase classQueryUseCase;
    private SlotManagementUseCase slotManagementUseCase;
    private SlotQueryUseCase slotQueryUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        adminAuthUseCase = mock(AdminAuthUseCase.class);
        adminCredentialUseCase = mock(AdminCredentialUseCase.class);
        adminMfaUseCase = mock(AdminMfaUseCase.class);
        adminSetupUseCase = mock(AdminSetupUseCase.class);
        productAdminUseCase = mock(ProductAdminUseCase.class);
        productQueryUseCase = mock(ProductQueryUseCase.class);
        workshopProfileUseCase = mock(WorkshopProfileUseCase.class);
        classManagementUseCase = mock(ClassManagementUseCase.class);
        classQueryUseCase = mock(ClassQueryUseCase.class);
        slotManagementUseCase = mock(SlotManagementUseCase.class);
        slotQueryUseCase = mock(SlotQueryUseCase.class);

        ProductQueryUseCase.ProductWithInventory product = RestDocsFixtures.productWithInventory();
        ProductQueryUseCase.ProductWithInventory inactiveProduct =
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
        when(productAdminUseCase.register(
                any(), any(), any(), anyLong(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(new ProductAdminUseCase.ProductInventoryResult(
                        product.product(), product.inventory()));
        when(productQueryUseCase.listAllProducts()).thenReturn(List.of(product));
        when(productAdminUseCase.update(
                eq(1L), any(), any(), anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(new ProductAdminUseCase.ProductInventoryResult(
                        product.product(), product.inventory()));
        when(productAdminUseCase.changeStatus(1L, ProductStatus.INACTIVE))
                .thenReturn(new ProductAdminUseCase.ProductInventoryResult(
                        inactiveProduct.product(), inactiveProduct.inventory()));
        when(productAdminUseCase.adjustInventory(any())).thenReturn(inventoryAdjustment);
        when(productAdminUseCase.listRecentInventoryAdjustments(1L))
                .thenReturn(List.of(inventoryAdjustment));
        when(workshopProfileUseCase.get()).thenReturn(workshop);
        when(workshopProfileUseCase.update(any())).thenReturn(workshop);
        when(classManagementUseCase.createClass(any())).thenReturn(bookingClass);
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

        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
                new AdminLoginController(adminAuthUseCase, new AdminBearerTokenResolver()),
                new AdminCredentialController(adminCredentialUseCase),
                new AdminMfaController(adminMfaUseCase),
                new AdminSetupController(new AdminSetupProperties("setup-token"), adminSetupUseCase),
                new AdminProductController(productAdminUseCase, productQueryUseCase),
                new AdminWorkshopProfileController(workshopProfileUseCase),
                new AdminClassController(classManagementUseCase, classQueryUseCase),
                new AdminSlotController(slotManagementUseCase, slotQueryUseCase));
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
}
