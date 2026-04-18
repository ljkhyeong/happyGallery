package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.adapter.in.web.config.properties.AdminProperties;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;

class AdminAuthFilterTest {

    @DisplayName("v1 관리자 경로에 키가 없으면 401을 반환한다")
    @Test
    void returns401_whenNoAdminKeyOnVersionedAdminPath() throws Exception {
        AdminAuthFilter filter = createFilter(properties("dev-admin-key", true));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/orders/expire-pickups");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());
        String responseBody = response.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(401);
            softly.assertThat(responseBody).contains("\"code\":\"UNAUTHORIZED\"");
        });
    }

    @DisplayName("v1 관리자 경로에 올바른 키를 주면 통과한다")
    @Test
    void passes_whenValidAdminKeyOnVersionedAdminPath() throws Exception {
        AdminAuthFilter filter = createFilter(properties("dev-admin-key", true));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/orders/expire-pickups");
        request.addHeader("X-Admin-Key", "dev-admin-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(200);
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR)).isNull();
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_USERNAME_ATTR)).isNull();
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_AUTH_SOURCE_ATTR))
                    .isEqualTo(AdminAuthFilter.AdminAuthSource.API_KEY);
        });
    }

    @DisplayName("Bearer 토큰이 유효하면 통과하고 admin 정보가 request attribute에 설정된다")
    @Test
    void passes_whenValidBearerToken() throws Exception {
        AdminAuthUseCase authUseCase = Mockito.mock(AdminAuthUseCase.class);
        Mockito.when(authUseCase.validateToken(anyString()))
                .thenReturn(Optional.of(new AdminSession(1L, "admin", Instant.now())));
        AdminAuthFilter filter = new AdminAuthFilter(properties("dev-admin-key", false), authUseCase, new ObjectMapper());
        String token = "valid-token";

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/products");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(200);
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR)).isEqualTo(1L);
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_USERNAME_ATTR)).isEqualTo("admin");
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_AUTH_SOURCE_ATTR))
                    .isEqualTo(AdminAuthFilter.AdminAuthSource.BEARER_SESSION);
        });
    }

    @DisplayName("API Key 비활성화 시 X-Admin-Key로는 인증되지 않는다")
    @Test
    void returns401_whenApiKeyAuthDisabledAndOnlyAdminKeyProvided() throws Exception {
        AdminAuthFilter filter = createFilter(properties("dev-admin-key", false));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/products");
        request.addHeader("X-Admin-Key", "dev-admin-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @DisplayName("로그인 경로(/admin/auth/)는 인증 없이 통과한다")
    @Test
    void passes_authPathWithoutAuthentication() throws Exception {
        AdminAuthFilter filter = createFilter(properties("dev-admin-key", false));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("회원 경로(/api/v1/me/)는 인증 없이 AdminAuthFilter를 통과한다")
    @Test
    void passes_customerMePath_withoutAuthentication() throws Exception {
        AdminAuthFilter filter = createFilter(properties("dev-admin-key", true));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/me/bookings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("공개 경로(/api/v1/auth/)는 인증 없이 AdminAuthFilter를 통과한다")
    @Test
    void passes_publicAuthPath_withoutAuthentication() throws Exception {
        AdminAuthFilter filter = createFilter(properties("dev-admin-key", true));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/signup");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("기본 설정에서 API Key 인증이 비활성화되어 있다")
    @Test
    void defaultProperties_apiKeyAuthDisabled() {
        AdminProperties defaults = new AdminProperties("", false);
        assertSoftly(softly -> {
            softly.assertThat(defaults.enableApiKeyAuth()).as("enableApiKeyAuth default").isFalse();
            softly.assertThat(defaults.apiKey()).as("apiKey default").isEmpty();
        });
    }

    @DisplayName("기본 설정으로 admin API에 접근하면 401을 반환한다")
    @Test
    void defaultProperties_adminAccess_returns401() throws Exception {
        AdminAuthFilter filter = createFilter(new AdminProperties("", false));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/bookings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
    }

    private static AdminAuthFilter createFilter(AdminProperties props) {
        return new AdminAuthFilter(props, Mockito.mock(AdminAuthUseCase.class), new ObjectMapper());
    }

    private static AdminProperties properties(String apiKey, boolean enableApiKeyAuth) {
        return new AdminProperties(apiKey, enableApiKeyAuth);
    }
}
