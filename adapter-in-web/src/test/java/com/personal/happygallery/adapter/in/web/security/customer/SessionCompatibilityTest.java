package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.adapter.in.web.customer.CustomerSessionBinder;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * df0d4eade56977562a842d2dc54fb68beb6ecc0a의 세션 계약을 고정한다.
 * 현재 writer로 fixture를 생성하지 않는다. 형식 변경 시 기존 fixture를 덮어쓰지 않는다.
 */
class SessionCompatibilityTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SessionStateCodec codec = new SessionStateCodec(mapper);
    private static final String SIGNUP = """
            {"termsVersion":"2026-09-11-v1","termsAccepted":true,
             "privacyVersion":"2026-09-11-v1","privacyAccepted":true,
             "provider":"NAVER","attemptId":"legacy-attempt","oauthState":"legacy-state",
             "expiresAt":"2026-09-21T00:05:00Z"}
            """;
    private static final String LINK = """
            {"userId":42,"credentialVersion":3,"provider":"NAVER",
             "expiresAt":"2026-09-21T00:05:00Z","attemptId":"legacy-attempt",
             "oauthState":"legacy-state","purpose":"%s"}
            """;
    private static final String STEP_UP = """
            {"userId":42,"credentialVersion":3,"expiresAt":"2026-09-21T00:10:00Z"}
            """;

    @Test
    @DisplayName("이전 가입 intent를 소비하며 새 writer도 이전 JSON 계약을 유지한다")
    void signupContract() {
        var store = new SocialSignupIntentStore(clock, codec);
        var request = callback();
        request.getSession().setAttribute("socialSignupIntent", SIGNUP);
        var policies = new PolicyAcceptance("2026-09-11-v1", true, "2026-09-11-v1", true);
        assertThat(store.consume(request, SocialProvider.NAVER)).contains(policies);
        assertThat(store.consume(request, SocialProvider.NAVER)).isEmpty();

        String attempt = store.start(request, SocialProvider.NAVER, policies);
        assertThat(store.bindOauthState(request, attempt, SocialProvider.NAVER, "legacy-state")).isTrue();
        assertStoredContract(request, "socialSignupIntent", SIGNUP.replace("legacy-attempt", attempt));
    }

    @ParameterizedTest
    @EnumSource(SocialAccountLinkIntentStore.IntentPurpose.class)
    @DisplayName("이전 계정 연결·재인증 intent와 새 writer의 양방향 저장 계약을 유지한다")
    void linkContract(SocialAccountLinkIntentStore.IntentPurpose purpose) {
        var store = new SocialAccountLinkIntentStore(clock, codec);
        var request = callback();
        request.getSession().setAttribute("customerUserId", 42L);
        request.getSession().setAttribute("customerCredentialVersion", 3L);
        String legacy = LINK.formatted(purpose.name());
        request.getSession().setAttribute("socialAccountLinkIntent", legacy);
        assertThat(store.consume(request, SocialProvider.NAVER))
                .contains(new SocialAccountLinkIntentStore.LinkIntent(42L, 3L, purpose));
        assertThat(store.consume(request, SocialProvider.NAVER)).isEmpty();

        String attempt = purpose == SocialAccountLinkIntentStore.IntentPurpose.LINK
                ? store.start(request, 42L, 3L, SocialProvider.NAVER)
                : store.startReauthentication(request, 42L, 3L, SocialProvider.NAVER);
        assertThat(store.bindOauthState(request, attempt, SocialProvider.NAVER, "legacy-state")).isTrue();
        assertStoredContract(request, "socialAccountLinkIntent", legacy.replace("legacy-attempt", attempt));
    }

    @Test
    @DisplayName("이전 재인증 증명을 읽고 새 증명도 같은 계약을 쓰며 자격 버전 변경은 거절한다")
    void stepUpContract() {
        var store = new CustomerStepUpAuthenticationStore(clock, codec);
        var request = callback();
        request.getSession().setAttribute("customerStepUpAuthentication", STEP_UP);
        assertThat(store.isRecentlyVerified(request, 42L, 3L)).isTrue();
        assertThat(store.isRecentlyVerified(request, 42L, 4L)).isFalse();
        assertThat(request.getSession().getAttribute("customerStepUpAuthentication")).isNull();

        store.markVerified(request, 42L, 3L);
        assertStoredContract(request, "customerStepUpAuthentication", STEP_UP);
    }

    @Test
    @DisplayName("이전 로그인 세션 키를 인증하며 새 로그인도 동일한 키와 Long 값을 저장한다")
    void authenticatedSessionContract() throws Exception {
        var user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getCredentialVersion()).thenReturn(3L);
        var auth = mock(CustomerAuthUseCase.class);
        when(auth.findUser(42L)).thenReturn(Optional.of(user));
        var request = callback();
        request.getSession().setAttribute("customerUserId", 42L);
        request.getSession().setAttribute("customerCredentialVersion", 3L);
        var response = new MockHttpServletResponse();
        var filter = new CustomerAuthenticationFilter(auth, ignored -> true);
        try {
            filter.doFilter(request, response, (incoming, outgoing) -> {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                assertThat(authentication).isNotNull();
                assertThat(((CustomerPrincipal) authentication.getPrincipal()).userId()).isEqualTo(42L);
            });
        } finally {
            SecurityContextHolder.clearContext();
        }

        var fresh = new MockHttpServletRequest();
        new CustomerSessionBinder(mock(CsrfTokenRepository.class),
                new CustomerStepUpAuthenticationStore(clock, codec)).bind(fresh, response, user);
        assertThat(fresh.getSession().getAttribute("customerUserId")).isEqualTo(42L);
        assertThat(fresh.getSession().getAttribute("customerCredentialVersion")).isEqualTo(3L);
        assertThat(fresh.getSession().getAttribute(
                "org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME"))
                .isEqualTo("42:3");
    }

    private MockHttpServletRequest callback() {
        var request = new MockHttpServletRequest();
        request.addParameter("state", "legacy-state");
        return request;
    }

    private void assertStoredContract(MockHttpServletRequest request, String key, String legacy) {
        Object stored = request.getSession().getAttribute(key);
        assertThat(stored).isInstanceOf(String.class);
        assertThat(mapper.readTree((String) stored)).isEqualTo(mapper.readTree(legacy));
    }
}
