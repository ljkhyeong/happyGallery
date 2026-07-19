package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.RegisterInitialPhoneRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class MemberPhoneRegistrationUseCaseIT {

    private static final String PHONE = "01012345678";

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired SocialAuthUseCase socialAuth;
    @Autowired GuestBookingUseCase guestBookingUseCase;
    @Autowired UserReaderPort userReader;
    @Autowired TestCleanupSupport cleanupSupport;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearPassData();
        cleanupSupport.clearUsers();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearPassData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("소셜 신규 회원은 결제 전에 인증한 휴대폰 번호를 최초 등록한다")
    @Test
    void socialMemberRegistersVerifiedPhoneBeforePayment() throws Exception {
        User socialUser = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.NAVER,
                "phone-onboarding-naver-id",
                "phone-onboarding@example.com",
                "소셜 회원")).user();
        MockHttpSession session = customerSession(socialUser);
        String prepareBody = """
                {
                  "context": "PASS",
                  "payload": {
                    "type": "PASS",
                    "userId": %d
                  }
                }
                """.formatted(socialUser.getId());

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prepareBody))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_REQUIRED"));

        String code = guestBookingUseCase.sendVerificationCode(PHONE).getCode();
        mockMvc.perform(patch("/api/v1/me/phone")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterInitialPhoneRequest(PHONE, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(PHONE))
                .andExpect(jsonPath("$.phoneVerified").value(true));

        User registered = userReader.findById(socialUser.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(registered.getPhone()).isEqualTo(PHONE);
            softly.assertThat(registered.isPhoneVerified()).isTrue();
            softly.assertThat(registered.getPhoneEnc()).isNotEqualTo(PHONE);
            softly.assertThat(registered.getPhoneHmac()).isNotBlank();
        });

        mockMvc.perform(post("/api/v1/payments/prepare")
                        .with(csrf())
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prepareBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value("PASS"));
    }

    private MockHttpSession customerSession(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE,
                user.getId());
        session.setAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE,
                user.getCredentialVersion());
        return session;
    }
}
