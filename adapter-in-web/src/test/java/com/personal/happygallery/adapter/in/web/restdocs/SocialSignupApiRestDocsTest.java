package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.CustomerSessionBinder;
import com.personal.happygallery.adapter.in.web.customer.SocialSignupController;
import com.personal.happygallery.adapter.in.web.security.customer.PendingSocialSignupStore;
import com.personal.happygallery.adapter.in.web.security.customer.SessionStateCodec;
import com.personal.happygallery.adapter.in.web.security.customer.SocialSignupIntentStore;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.policy.PolicyConsentProperties;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.application.policy.port.out.PolicyConsentStorePort;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SocialSignupApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;
    private PendingSocialSignupStore pending;
    private static final String POLICY = """
            {"termsVersion":"2026-09-11-v1","termsAccepted":true,
             "privacyVersion":"2026-09-12-v1","privacyAccepted":true}
            """;

    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        Clock clock = Clock.systemUTC();
        var codec = new SessionStateCodec(JsonMapper.builder().build());
        pending = new PendingSocialSignupStore(clock, codec);
        var auth = mock(SocialAuthUseCase.class);
        when(auth.socialLogin(any())).thenReturn(new SocialAuthUseCase.SocialLoginResult(
                User.fromSocialProfile(null, "네이버 회원"), true));
        mvc = mockMvc(documentation, new SocialSignupController(
                new PolicyConsentService(new PolicyConsentProperties("2026-09-11-v1", "2026-09-12-v1"),
                        mock(PolicyConsentStorePort.class), clock),
                new SocialSignupIntentStore(clock, codec), pending, auth, mock(CustomerSessionBinder.class)));
    }

    @Test
    @DisplayName("소셜 인증 전 가입 동의를 등록하는 API를 문서화한다")
    void startSignup() throws Exception {
        mvc.perform(post("/api/v1/auth/social/signup-intents/naver").contentType(APPLICATION_JSON).content(POLICY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorizationUrl").isString());
    }

    @Test
    @DisplayName("소셜 인증 후 동의로 가입을 완료하는 API를 문서화한다")
    void completeSignup() throws Exception {
        var request = new MockHttpServletRequest();
        String attempt = pending.save(request,
                new SocialAuthUseCase.SocialLoginCommand(SocialProvider.NAVER, "naver-member", null, "네이버 회원"));
        mvc.perform(post("/api/v1/auth/social/signup-completion")
                        .session((MockHttpSession) request.getSession()).contentType(APPLICATION_JSON)
                        .content("{\"attemptId\":\"" + attempt + "\",\"policyAcceptance\":" + POLICY + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("네이버 회원"));
    }

    @Test
    @DisplayName("가입 대기가 없거나 동의 버전이 다르면 오류를 반환한다")
    void rejectsInvalidCompletion() throws Exception {
        mvc.perform(post("/api/v1/auth/social/signup-completion").contentType(APPLICATION_JSON)
                        .content("{\"attemptId\":\"missing\",\"policyAcceptance\":" + POLICY + "}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("SOCIAL_LOGIN_FAILED"));
    }
}
