package com.personal.happygallery.app.web;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort;
import com.personal.happygallery.app.web.admin.AdminSessionStore;
import com.personal.happygallery.config.properties.AdminProperties;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("Bearer 토큰이 유효하면 통과하고 admin 정보가 request attribute에 설정된다")
    @Test
    void passes_whenValidBearerToken() throws Exception {
        AdminSessionStore store = workingAdminSessionStore();
        String token = store.create(1L, "admin");
        AdminAuthFilter filter = new AdminAuthFilter(properties("dev-admin-key", false), store);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/products");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(200);
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR)).isEqualTo(1L);
            softly.assertThat(request.getAttribute(AdminAuthFilter.ADMIN_USERNAME_ATTR)).isEqualTo("admin");
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

    private static AdminAuthFilter createFilter(AdminProperties props) {
        return new AdminAuthFilter(props, Mockito.mock(AdminSessionPort.class));
    }

    // -----------------------------------------------------------------------
    // Q1-T6: customer auth 경로는 AdminAuthFilter를 통과한다
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Q1-T7: unsafe default 제거 회귀 테스트
    // -----------------------------------------------------------------------

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

    /**
     * 인메모리 map으로 Redis 저장소를 흉내 내는 AdminSessionStore.
     * Bearer 토큰 검증 테스트에서 실제 Redis 없이 create/validate 동작을 검증할 수 있게 한다.
     */
    @SuppressWarnings("unchecked")
    private static AdminSessionStore workingAdminSessionStore() {
        ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
        StringRedisTemplate redis = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(redis.opsForValue()).thenReturn(ops);
        Mockito.doAnswer(inv -> { store.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(ops).set(anyString(), anyString(), any(Duration.class));
        Mockito.when(ops.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0)));
        Mockito.doNothing().when(redis).delete(anyString());
        return new AdminSessionStore(redis, new ObjectMapper());
    }

    // -----------------------------------------------------------------------
    // Q1-T6: customer auth 경로는 AdminAuthFilter를 통과한다
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Q1-T7: unsafe default 제거 회귀 테스트
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Q1-T6: customer auth 경로는 AdminAuthFilter를 통과한다
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Q1-T7: unsafe default 제거 회귀 테스트
    // -----------------------------------------------------------------------

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

    private static AdminProperties properties(String apiKey, boolean enableApiKeyAuth) {
        return new AdminProperties(apiKey, enableApiKeyAuth);
    }
}
