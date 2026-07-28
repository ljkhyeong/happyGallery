package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import java.time.Clock;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static com.personal.happygallery.adapter.in.web.security.customer.SocialAccountLinkIntentStore.IntentPurpose.LINK;
import static com.personal.happygallery.adapter.in.web.security.customer.SocialAccountLinkIntentStore.IntentPurpose.REAUTHENTICATE;

class SocialAccountLinkIntentStoreTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC);
    private final SocialAccountLinkIntentStore store = new SocialAccountLinkIntentStore(clock);
    private final SocialSignupIntentStore signupIntentStore = new SocialSignupIntentStore(clock);

    @DisplayName("연결 시작 요청은 Spring이 생성한 OAuth state와 결합한 뒤 같은 회원 callback에서만 소비한다")
    @Test
    void bindsGeneratedOauthState() {
        MockHttpServletRequest authorizationRequest = authenticatedRequest();
        String attemptId = store.start(authorizationRequest, 1L, 3L, SocialProvider.GOOGLE);
        authorizationRequest.addParameter(SocialAccountLinkIntentStore.LINK_ATTEMPT_PARAMETER, attemptId);
        var resolver = new SocialOAuth2AuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(googleRegistration()),
                store,
                signupIntentStore);

        var oauthRequest = resolver.resolve(authorizationRequest);
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) authorizationRequest.getSession(false), oauthRequest.getState());

        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE))
                .contains(new SocialAccountLinkIntentStore.LinkIntent(1L, 3L, LINK));
    }

    @DisplayName("소셜 재인증 의도는 계정 연결 의도와 구분해 callback까지 보존한다")
    @Test
    void preservesReauthenticationPurpose() {
        MockHttpServletRequest authorizationRequest = authenticatedRequest();
        String attemptId = store.startReauthentication(
                authorizationRequest, 1L, 3L, SocialProvider.GOOGLE);
        assertThat(store.bindOauthState(
                authorizationRequest, attemptId, SocialProvider.GOOGLE, "bound-state")).isTrue();
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) authorizationRequest.getSession(false), "bound-state");

        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE))
                .contains(new SocialAccountLinkIntentStore.LinkIntent(
                        1L, 3L, REAUTHENTICATE));
    }

    @DisplayName("OAuth callback 전에 세션 회원이 바뀌면 연결 의도를 거절하고 폐기한다")
    @Test
    void rejectsReboundCustomerSession() {
        MockHttpServletRequest authorizationRequest = authenticatedRequest();
        String attemptId = store.start(authorizationRequest, 1L, 3L, SocialProvider.GOOGLE);
        assertThat(store.bindOauthState(
                authorizationRequest, attemptId, SocialProvider.GOOGLE, "bound-state")).isTrue();
        MockHttpSession session = (MockHttpSession) authorizationRequest.getSession(false);
        session.setAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE, 2L);
        MockHttpServletRequest callbackRequest = callbackRequest(session, "bound-state");

        assertThatThrownBy(() -> store.consume(callbackRequest, SocialProvider.GOOGLE))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE)).isEmpty();
    }

    @DisplayName("연결 의도와 다른 제공자 callback은 거절하고 의도를 즉시 폐기한다")
    @Test
    void rejectsDifferentProviderAndConsumesIntent() {
        MockHttpServletRequest request = authenticatedRequest();
        String attemptId = store.start(request, 1L, 3L, SocialProvider.GOOGLE);
        assertThat(store.bindOauthState(request, attemptId, SocialProvider.GOOGLE, "bound-state")).isTrue();
        MockHttpServletRequest callbackRequest = callbackRequest(
                (MockHttpSession) request.getSession(false), "bound-state");

        assertThatThrownBy(() -> store.consume(callbackRequest, SocialProvider.NAVER))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SOCIAL_LOGIN_FAILED);
        assertThat(store.consume(callbackRequest, SocialProvider.GOOGLE)).isEmpty();
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/auth/social/authorization/google");
        request.setServletPath("/api/v1/auth/social/authorization/google");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE, 1L);
        session.setAttribute(CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE, 3L);
        request.setSession(session);
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
