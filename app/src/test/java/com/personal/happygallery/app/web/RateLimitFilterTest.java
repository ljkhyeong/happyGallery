package com.personal.happygallery.app.web;

import com.personal.happygallery.config.properties.RateLimitProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("제한 대상이 아닌 경로는 처리율 제한 없이 통과한다")
    @Test
    void passesThrough_whenPathIsNotRateLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 1, 1, 1, 1));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("동일 IP에서 인증코드 발송 요청이 초과되면 429를 반환한다")
    @Test
    void returns429_whenPhoneVerificationLimitExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 1, 10, 10, 10));

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/bookings/phone-verifications");
        first.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/bookings/phone-verifications");
        second.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new MockFilterChain());
        String secondResponseBody = secondResponse.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
            softly.assertThat(secondResponseBody).contains("\"code\":\"TOO_MANY_REQUESTS\"");
        });
    }

    @DisplayName("레거시와 v1 관리자 경로는 동일한 처리율 제한 규칙을 사용한다")
    @Test
    void appliesSameLimit_forLegacyAndV1AdminPaths() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 10, 10, 10, 1));

        MockHttpServletRequest legacyPathRequest = new MockHttpServletRequest("POST", "/admin/orders/expire-pickups");
        legacyPathRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(legacyPathRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest v1PathRequest = new MockHttpServletRequest("POST", "/api/v1/admin/orders/expire-pickups");
        v1PathRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(v1PathRequest, secondResponse, new MockFilterChain());
        String secondResponseBody = secondResponse.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponseBody).contains("\"code\":\"TOO_MANY_REQUESTS\"");
        });
    }

    private static RateLimitProperties properties(boolean enabled,
                                                  long phoneVerificationPerSecond,
                                                  long bookingCreatePerMinute,
                                                  long passPurchasePerMinute,
                                                  long adminApiPerMinute) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(enabled);
        properties.setPhoneVerificationPerSecond(phoneVerificationPerSecond);
        properties.setBookingCreatePerMinute(bookingCreatePerMinute);
        properties.setPassPurchasePerMinute(passPurchasePerMinute);
        properties.setAdminApiPerMinute(adminApiPerMinute);
        return properties;
    }
}
