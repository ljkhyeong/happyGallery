package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import tools.jackson.databind.json.JsonMapper;

import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialSignupIntentStoreTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC);
    private final SessionStateCodec stateCodec =
            new SessionStateCodec(JsonMapper.builder().build());
    private final SocialSignupIntentStore store = new SocialSignupIntentStore(clock, stateCodec);
    private final SocialAccountLinkIntentStore linkIntentStore =
            new SocialAccountLinkIntentStore(clock, stateCodec);

    @DisplayName("가입 동의 intent는 제공자와 OAuth state에 결합해 callback에서 한 번만 소비한다")
    @Test
    void bindsProviderAndGeneratedOauthStateAndConsumesOnce() {
        MockHttpServletRequest authorizationRequest = authorizationRequest(SocialProvider.GOOGLE);
        String attemptId = store.start(authorizationRequest, SocialProvider.GOOGLE, acceptedPolicies());
        authorizationRequest.addParameter(SocialSignupIntentStore.SIGNUP_ATTEMPT_PARAMETER, attemptId);
        var resolver = resolver();

        var oauthRequest = resolver.resolve(authorizationRequest);
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) authorizationRequest.getSession(false), oauthRequest.getState());

        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE)).contains(acceptedPolicies());
        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE)).isEmpty();
    }

    @DisplayName("가입 intent와 다른 제공자 callback은 거절하고 intent를 폐기한다")
    @Test
    void rejectsDifferentProviderAndConsumesIntent() {
        MockHttpServletRequest request = authorizationRequest(SocialProvider.GOOGLE);
        String attemptId = store.start(request, SocialProvider.GOOGLE, acceptedPolicies());
        assertThat(store.bindOauthState(
                request, attemptId, SocialProvider.GOOGLE, "bound-state")).isTrue();
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) request.getSession(false), "bound-state");

        assertThatThrownBy(() -> store.consume(callbackRequest, SocialProvider.NAVER))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE)).isEmpty();
    }

    @DisplayName("5분이 지난 가입 intent는 callback에서 거절하고 폐기한다")
    @Test
    void rejectsExpiredIntent() {
        MockHttpServletRequest request = authorizationRequest(SocialProvider.GOOGLE);
        String attemptId = store.start(request, SocialProvider.GOOGLE, acceptedPolicies());
        assertThat(store.bindOauthState(
                request, attemptId, SocialProvider.GOOGLE, "bound-state")).isTrue();
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) request.getSession(false), "bound-state");
        SocialSignupIntentStore expiredStore =
                new SocialSignupIntentStore(
                        Clock.offset(clock, Duration.ofMinutes(5)),
                        stateCodec);

        assertThatThrownBy(() -> expiredStore.consume(callbackRequest, SocialProvider.GOOGLE))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    @DisplayName("OAuth 시작 GET의 과거 약관 query는 가입 동의 intent로 취급하지 않는다")
    @Test
    void ignoresLegacyPolicyQueryParameters() {
        MockHttpServletRequest request = authorizationRequest(SocialProvider.GOOGLE);
        request.addParameter("termsVersion", "2026-08-08-v1");
        request.addParameter("termsAccepted", "true");
        request.addParameter("privacyVersion", "2026-08-11-v2");
        request.addParameter("privacyAccepted", "true");

        var oauthRequest = resolver().resolve(request);
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) request.getSession(false), oauthRequest.getState());

        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE)).isEmpty();
    }

    private SocialOAuth2AuthorizationRequestResolver resolver() {
        return new SocialOAuth2AuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(googleRegistration()),
                linkIntentStore,
                store);
    }

    private MockHttpServletRequest authorizationRequest(SocialProvider provider) {
        String providerPath = provider.name().toLowerCase();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/auth/social/authorization/" + providerPath);
        request.setServletPath("/api/v1/auth/social/authorization/" + providerPath);
        request.setSession(new MockHttpSession());
        return request;
    }

    private MockHttpServletRequest callbackRequest(MockHttpSession session, String state) {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/auth/social/callback/google");
        request.setSession(session);
        request.addParameter(OAuth2ParameterNames.STATE, state);
        return request;
    }

    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("google-client")
                .clientSecret("google-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/api/v1/auth/social/callback/google")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }
}
