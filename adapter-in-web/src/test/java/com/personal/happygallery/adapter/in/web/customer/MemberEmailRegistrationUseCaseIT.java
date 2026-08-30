package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.RegisterEmailRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SendEmailVerificationRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.out.EmailVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.acceptedPolicies;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MemberEmailRegistrationUseCaseIT {

    private static final String EMAIL = "Naver.Member@Example.com";

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired SocialAuthUseCase socialAuth;
    @Autowired EmailVerificationReaderPort verificationReader;
    @Autowired UserReaderPort userReader;
    @Autowired CustomerStepUpAuthenticationStore stepUpAuthenticationStore;
    @Autowired TestCleanupSupport cleanupSupport;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("네이버 전용 회원은 메일함 인증 후 이메일을 등록하고 기존 세션을 폐기한다")
    void naverMemberRegistersVerifiedEmail() throws Exception {
        User user = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "email-registration-naver-id",
                "ignored-profile-email@naver.com",
                "네이버 회원",
                acceptedPolicies())).user();
        long credentialVersion = user.getCredentialVersion();
        MockHttpSession session = customerSession(user);

        mockMvc.perform(post("/api/v1/me/email-verifications")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendEmailVerificationRequest(EMAIL))))
                .andExpect(status().isNoContent());

        String normalizedEmail = EMAIL.toLowerCase(Locale.ROOT);
        String code = verificationReader
                .findLatestUnverifiedCode(user.getId(), normalizedEmail)
                .orElseThrow()
                .getCode();

        mockMvc.perform(patch("/api/v1/me/email")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterEmailRequest(EMAIL, code))))
                .andExpect(status().isNoContent());

        User registered = userReader.findById(user.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(registered.getEmail()).isEqualTo(normalizedEmail);
            softly.assertThat(registered.getEmailEnc()).isNotEqualTo(normalizedEmail);
            softly.assertThat(registered.getEmailHmac()).isNotBlank();
            softly.assertThat(registered.getCredentialVersion())
                    .isEqualTo(credentialVersion + 1);
            softly.assertThat(registered.hasLocalPassword()).isFalse();
            softly.assertThat(verificationReader
                    .findLatestUnverifiedCode(user.getId(), normalizedEmail))
                    .isEmpty();
        });

        mockMvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession customerSession(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE,
                user.getId());
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE,
                user.getCredentialVersion());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        stepUpAuthenticationStore.markVerified(
                request, user.getId(), user.getCredentialVersion());
        return session;
    }
}
