package com.personal.happygallery.adapter.in.web.security.customer;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SocialPhoneProfileTest {
    private final SocialOAuth2ProfileResolver resolver = new SocialOAuth2ProfileResolver();

    @Test
    @DisplayName("네이버 휴대전화번호와 카카오 국제 형식 번호를 국내 번호로 정규화한다")
    void normalizesProviderPhones() {
        var naver = token("naver", Map.of("id", "naver-id", "name", "회원", "mobile", "010-8123-4567"));
        assertThat(resolver.resolveLogin(naver).providerPhone()).isEqualTo("01081234567");
        assertThat(resolver.resolveLogin(kakao("+82 10-8123-4567", false)).providerPhone())
                .isEqualTo("01081234567");
    }

    @Test
    @DisplayName("번호 동의가 없거나 지원하지 않는 번호이면 기존 로그인용 프로필에는 번호를 담지 않는다")
    void rejectsUnconsentedAndForeignNumbers() {
        assertThat(resolver.resolveLogin(kakao("+82 10-8123-4567", true)).providerPhone()).isNull();
        assertThat(resolver.resolveLogin(kakao("+1 202-555-0100", false)).providerPhone()).isNull();
        assertThat(resolver.resolveLogin(token("naver", Map.of("id", "naver-id", "name", "회원")))
                .providerPhone()).isNull();
    }

    @Test
    @DisplayName("카카오 인증 요청은 기존 scope와 state를 유지하고 전화번호 동의를 요청한다")
    void requestsKakaoPhoneScope() {
        var registration = ClientRegistration.withRegistrationId("kakao").clientId("client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/callback")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .scope("profile_nickname", "account_email").build();
        var codec = new SessionStateCodec(JsonMapper.builder().build());
        var requestResolver = new SocialOAuth2AuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(registration),
                new SocialAccountLinkIntentStore(Clock.systemUTC(), codec),
                new SocialSignupIntentStore(Clock.systemUTC(), codec));
        var request = new MockHttpServletRequest("GET", "/api/v1/auth/social/authorization/kakao");
        request.setServletPath(request.getRequestURI());
        var result = requestResolver.resolve(request);
        assertThat(result).isNotNull();
        assertThat(result.getScopes()).containsExactlyInAnyOrder("profile_nickname", "account_email", "phone_number");
        assertThat(result.getAuthorizationRequestUri()).contains("phone_number", "state=");
        assertThat(result.getState()).isNotBlank();
    }

    private OAuth2AuthenticationToken kakao(String phone, boolean needsAgreement) {
        return token("kakao", Map.of("id", 123L, "nickname", "회원", "email", "user@example.com",
                "is_email_valid", true, "is_email_verified", true,
                "phone_number", phone, "phone_number_needs_agreement", needsAgreement));
    }

    private OAuth2AuthenticationToken token(String provider, Map<String, Object> attributes) {
        var user = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), provider);
    }
}
