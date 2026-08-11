package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.admin.dto.LoginRequest;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class SecurityBoundaryUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ObjectMapper objectMapper;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired AdminUserPort adminUserPort;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired FilterChainProxy springSecurityFilterChain;
    @Autowired
    @Qualifier("rateLimitFilterRegistration")
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
        cleanupSupport.clearAdminUsers();
    }

    // csrf() 후처리기는 캐시된 테스트 컨텍스트의 공유 저장소를 바꾸므로 새 컨텍스트에서 검증한다.
    @DisplayName("발급받은 SPA CSRF 쿠키를 헤더로 보내면 상태 변경 요청이 통과한다")
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void csrfToken_allowsStateChangingRequest() throws Exception {
        var tokenResult = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cookieName").value("XSRF-TOKEN"))
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andReturn();

        Cookie tokenCookie = tokenResult.getResponse().getCookie("XSRF-TOKEN");
        JsonNode tokenBody = objectMapper.readTree(tokenResult.getResponse().getContentAsString());

        assertThat(tokenCookie).isNotNull();
        mockMvc.perform(post("/api/v1/monitoring/client-events")
                        .cookie(tokenCookie)
                        .header(tokenBody.get("headerName").asText(), tokenCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "GUEST_LOOKUP_HUB_VIEWED",
                                  "path": "/guest"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @DisplayName("CSRF 토큰 없이 상태 변경 요청을 보내면 JSON 403을 반환한다")
    @Test
    void missingCsrfToken_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/monitoring/client-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "GUEST_LOOKUP_HUB_VIEWED",
                                  "path": "/guest"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("회원 후기 상태 변경은 CSRF 토큰과 회원 세션을 모두 요구한다")
    @Test
    void memberReviewMutation_requiresCsrfAndCustomerSession() throws Exception {
        mockMvc.perform(put("/api/v1/me/reviews/1/helpful"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/v1/me/reviews/1/helpful").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("비회원도 현재 약관과 개인정보처리방침 버전을 조회할 수 있다")
    @Test
    void currentPolicies_allowAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/policies/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terms.version").value("2026-08-08-v1"))
                .andExpect(jsonPath("$.terms.documentPath").value("/terms/2026-08-08-v1"))
                .andExpect(jsonPath("$.privacy.version").value("2026-08-11-v1"))
                .andExpect(jsonPath("$.privacy.documentPath").value("/privacy/2026-08-11-v1"));
    }

    @DisplayName("공개 조회 경로는 HEAD 요청도 허용한다")
    @Test
    void publicReadEndpoint_allowsHeadRequest() throws Exception {
        mockMvc.perform(head("/api/v1/classes"))
                .andExpect(status().isOk());
        mockMvc.perform(head("/api/v1/products/1/reviews"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401)
                        .isNotEqualTo(403));
        mockMvc.perform(head("/api/v1/classes/1/reviews"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401)
                        .isNotEqualTo(403));
        mockMvc.perform(head("/api/v1/admin/setup/status"))
                .andExpect(status().isOk());
    }

    @DisplayName("등록되지 않은 조회 경로는 공개 namespace 아래에서도 거부한다")
    @Test
    void publicNamespace_deniesUnregisteredReadPath() throws Exception {
        mockMvc.perform(get("/api/v1/classes/1/internal"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/products/1/reviews/internal"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/auth/social/authorization/google/internal"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("공개 조회 namespace의 상태 변경 메서드는 거부한다")
    @Test
    void publicReadNamespace_deniesStateChangingMethod() throws Exception {
        mockMvc.perform(post("/api/v1/classes").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/v1/products/1/reviews").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("처리율 제한 필터는 자동 등록하지 않고 보안 헤더 필터 뒤에 배치한다")
    @Test
    void rateLimitFilter_runsInsideBothSecurityChainsAfterHeaderWriter() {
        assertThat(rateLimitFilterRegistration.isEnabled()).isFalse();
        assertThat(springSecurityFilterChain.getFilterChains())
                .hasSize(2)
                .allSatisfy(chain -> {
                    var filterTypes = chain.getFilters().stream()
                            .map(Object::getClass)
                            .toList();
                    assertThat(filterTypes).contains(HeaderWriterFilter.class, RateLimitFilter.class);
                    assertThat(filterTypes.indexOf(RateLimitFilter.class))
                            .isGreaterThan(filterTypes.indexOf(HeaderWriterFilter.class));
                });
    }

    @DisplayName("잘못된 Bearer 토큰은 유효한 관리자 API key로 폴백하지 않는다")
    @ParameterizedTest
    @ValueSource(strings = {
            "Bearer invalid-token",
            "bearer invalid-token",
            "Bearer"
    })
    void invalidBearer_doesNotFallBackToApiKey(String authorization) throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .header("X-Admin-Key", "dev-admin-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("로컬 API key는 계정 관리자 ID가 필요한 작업을 수행할 수 없다")
    @Test
    void apiKey_cannotPerformIdentifiedAdminOperation() throws Exception {
        mockMvc.perform(post("/api/v1/admin/order-claims/1/resolve")
                        .header("X-Admin-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": false,
                                  "refundAmount": null,
                                  "restoreInventory": false,
                                  "note": "처리 사유"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("로컬 API key는 후기 숨김과 재공개를 수행할 수 없다")
    @Test
    void apiKey_cannotModerateReview() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reviews/1/status")
                        .header("X-Admin-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "HIDDEN",
                                  "reason": "운영 정책 위반 내용",
                                  "expectedContentRevision": 1,
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("로컬 API key는 후기 공식 답글과 신고 결정을 수행할 수 없다")
    @Test
    void apiKey_cannotReplyOrDecideReviewReport() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/1/reply")
                        .header("X-Admin-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"content\":\"공식 답글입니다.\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/review-reports/1")
                        .header("X-Admin-Key", "dev-admin-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\",\"note\":null}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("유효한 관리자 Bearer 세션으로 보호 API를 호출할 수 있다")
    @Test
    void validBearerSession_authenticatesProtectedAdminRequest() throws Exception {
        adminUserPort.save(new AdminUser(
                "security-admin",
                passwordEncoder.encode("admin1234")));
        var loginResult = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("security-admin", "admin1234"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token")
                .asText();

        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @DisplayName("회원 개인정보 응답에는 중앙 캐시 금지 정책을 적용한다")
    @Test
    void customerProfile_disablesResponseCaching() throws Exception {
        Cookie customerSession = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader)
                .signupAndGetSessionCookie("cache-control@example.com", "010-1234-8888");

        mockMvc.perform(get("/api/v1/me").cookie(customerSession))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @DisplayName("회원 세션으로 관리자 API를 호출해도 회원 세션 ID는 바뀌지 않는다")
    @Test
    void adminAuthentication_doesNotRotateCustomerSession() throws Exception {
        Cookie customerSession = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader)
                .signupAndGetSessionCookie("security-boundary@example.com", "010-1234-9999");

        mockMvc.perform(get("/api/v1/admin/products")
                        .cookie(customerSession)
                        .header("X-Admin-Key", "dev-admin-key"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("HG_SESSION"));
    }
}
