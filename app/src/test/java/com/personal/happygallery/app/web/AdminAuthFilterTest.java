package com.personal.happygallery.app.web;

import com.personal.happygallery.app.web.admin.AdminSessionStore;
import com.personal.happygallery.config.properties.AdminProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Bearer 토큰이 유효하면 통과한다")
    @Test
    void passes_whenValidBearerToken() throws Exception {
        AdminSessionStore store = new AdminSessionStore();
        String token = store.create(1L, "admin");
        AdminAuthFilter filter = new AdminAuthFilter(properties("dev-admin-key", false), store);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/products");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
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

    private static AdminAuthFilter createFilter(AdminProperties props) {
        return new AdminAuthFilter(props, new AdminSessionStore());
    }

    private static AdminProperties properties(String apiKey, boolean enableApiKeyAuth) {
        AdminProperties properties = new AdminProperties();
        properties.setApiKey(apiKey);
        properties.setEnableApiKeyAuth(enableApiKeyAuth);
        return properties;
    }
}
