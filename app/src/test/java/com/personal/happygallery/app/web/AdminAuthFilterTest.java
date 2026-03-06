package com.personal.happygallery.app.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthFilterTest {

    @DisplayName("v1 관리자 경로에 키가 없으면 401을 반환한다")
    @Test
    void returns401_whenNoAdminKeyOnVersionedAdminPath() throws Exception {
        AdminAuthFilter filter = new AdminAuthFilter();
        ReflectionTestUtils.setField(filter, "adminApiKey", "dev-admin-key");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/orders/expire-pickups");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    @DisplayName("v1 관리자 경로에 올바른 키를 주면 통과한다")
    @Test
    void passes_whenValidAdminKeyOnVersionedAdminPath() throws Exception {
        AdminAuthFilter filter = new AdminAuthFilter();
        ReflectionTestUtils.setField(filter, "adminApiKey", "dev-admin-key");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/orders/expire-pickups");
        request.addHeader("X-Admin-Key", "dev-admin-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
