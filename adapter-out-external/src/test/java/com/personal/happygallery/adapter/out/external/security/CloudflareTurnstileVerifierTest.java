package com.personal.happygallery.adapter.out.external.security;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CloudflareTurnstileVerifierTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://challenges.cloudflare.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final CloudflareTurnstileVerifier verifier = new CloudflareTurnstileVerifier(properties(true), builder.build());

    @Test
    @DisplayName("토큰과 비밀 키만 전송하고 도메인과 용도가 일치한 응답을 허용한다")
    void validatesOfficialResponse() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"secret\":\"secret-key\",\"response\":\"token\"}"))
                .andRespond(withSuccess("""
                        {"success":true,"hostname":"happy-gallery.com","action":"phone_verification"}
                        """, MediaType.APPLICATION_JSON));
        assertThat(verifier.verify("token", "phone_verification")).isTrue();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
            "{\"success\":true,\"hostname\":\"other.example\",\"action\":\"phone_verification\"}",
            "{\"success\":true,\"hostname\":\"happy-gallery.com\",\"action\":\"group_inquiry\"}"
    })
    @DisplayName("만료·재사용 토큰과 다른 도메인·용도의 토큰을 거절한다")
    void rejectsUntrustedResponse(String body) {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThat(verifier.verify("token", "phone_verification")).isFalse();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "not-json", "{\"success\":false,\"error-codes\":[\"invalid-input-secret\"]}"})
    @DisplayName("잘못된 응답과 서비스 설정 오류는 본문 노출 없이 일시 장애로 처리한다")
    void invalidProviderResponse(String body) {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertUnavailable();
    }

    @Test
    @DisplayName("검증 서버 장애를 승인으로 처리하거나 자동 재요청하지 않는다")
    void unavailableProvider() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("secret response"));
        assertUnavailable();
    }

    @Test
    @DisplayName("비활성 상태는 공개 키를 반환하지 않고 활성화 시 빠진 키를 거절한다")
    void configuration() {
        assertThat(new CloudflareTurnstileVerifier(properties(false), builder.build()).siteKey()).isNull();
        assertThatThrownBy(() -> new TurnstileProperties(true, "", "", "happy-gallery.com",
                Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofMillis(500), 5, Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> verifier.verify("token", "phone_verification"))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE))
                .hasNoCause().hasMessageNotContaining("secret");
        server.verify();
    }

    private static TurnstileProperties properties(boolean enabled) {
        return new TurnstileProperties(enabled, "site-key", "secret-key", "happy-gallery.com",
                Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    }
}
