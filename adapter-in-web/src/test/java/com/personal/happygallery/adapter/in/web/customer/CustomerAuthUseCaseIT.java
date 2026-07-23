package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SignupRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.SocialLoginAuthenticationHandler;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class CustomerAuthUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ObjectMapper objectMapper;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired UserReaderPort userReader;
    @Autowired SocialLoginAuthenticationHandler socialLoginAuthenticationHandler;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @DisplayName("회원가입 후 사용자 정보와 세션 쿠키를 받는다")
    @Test
    void signup_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifiedSignupRequest(
                                "test@example.com", "테스트", "010-1234-5678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.phone").value("01012345678"))
                .andExpect(jsonPath("$.phoneVerified").value(true))
                .andExpect(jsonPath("$.provider").doesNotExist())
                .andExpect(cookie().exists("HG_SESSION"))
                .andExpect(cookie().httpOnly("HG_SESSION", true));
    }

    @DisplayName("중복 이메일로 회원가입하면 409를 반환한다")
    @Test
    void signup_duplicateEmail_conflict() throws Exception {
        SignupRequest request = verifiedSignupRequest(
                "dup@example.com", "테스트", "010-0000-0000");
        String firstBody = objectMapper.writeValueAsString(request);
        String duplicateBody = objectMapper.writeValueAsString(new SignupRequest(
                "DUP@EXAMPLE.COM", "password123", "테스트", "010-0000-0000",
                request.verificationCode()));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @DisplayName("인증 코드가 유효하지 않으면 회원가입을 거절한다")
    @Test
    void signup_rejectsInvalidPhoneVerification() throws Exception {
        String issuedCode = issueVerificationCode("01012345678");
        String invalidCode = issuedCode.equals("000000") ? "000001" : "000000";

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "invalid-code@example.com",
                                "password123",
                                "테스트",
                                "01012345678",
                                invalidCode))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PHONE_VERIFICATION_FAILED"));
    }

    @DisplayName("로그인 성공 시 사용자 정보와 세션 쿠키를 받는다")
    @Test
    void login_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifiedSignupRequest(
                                "login@example.com", "로그인", "010-1111-2222"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CustomerLoginRequest("LOGIN@EXAMPLE.COM", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(cookie().exists("HG_SESSION"));
    }

    @DisplayName("잘못된 비밀번호로 로그인하면 401을 반환한다")
    @Test
    void login_wrongPassword_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifiedSignupRequest(
                                "wrong@example.com", "테스트", "010-3333-4444"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CustomerLoginRequest("wrong@example.com", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @DisplayName("로그아웃 후 세션 쿠키가 삭제된다")
    @Test
    void logout_clearsCookie() throws Exception {
        var signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifiedSignupRequest(
                                "logout@example.com", "로그아웃", "010-9999-0000"))))
                .andExpect(status().isCreated())
                .andReturn();

        var sessionCookie = signupResult.getResponse().getCookie("HG_SESSION");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("HG_SESSION", 0));
    }

    @DisplayName("소셜 로그인 시작은 서버가 만든 state와 고정 callback으로 제공자에 이동한다")
    @Test
    void socialLogin_redirectsToProviderWithServerAuthorizationRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/social/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.LOCATION,
                        startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string(HttpHeaders.LOCATION,
                        containsString("state=")))
                .andExpect(header().string(HttpHeaders.LOCATION,
                        containsString("redirect_uri=")))
                .andExpect(cookie().exists("HG_SESSION"))
                .andReturn();
    }

    @DisplayName("소셜 로그인 callback의 state가 세션과 다르면 외부 토큰 교환 전에 실패한다")
    @Test
    void socialLogin_rejectsMismatchedState() throws Exception {
        var authorizationResult = mockMvc.perform(get("/api/v1/auth/social/authorization/naver"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        mockMvc.perform(get("/api/v1/auth/social/callback/naver")
                        .cookie(authorizationResult.getResponse().getCookie("HG_SESSION"))
                        .param("code", "unused-code")
                        .param("state", "wrong-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "/auth/callback?error=SOCIAL_LOGIN_FAILED"));
    }

    @DisplayName("검증된 Google 로그인은 회원 세션 하나로 전환된다")
    @Test
    void socialLogin_bindsCustomerSessionAfterVerifiedGoogleLogin() throws Exception {
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "google-id-token",
                now,
                now.plusSeconds(300),
                Map.of(
                        "sub", "google-account-id",
                        "email", "google@example.com",
                        "email_verified", true,
                        "name", "구글 사용자"));
        DefaultOidcUser principal = new DefaultOidcUser(List.of(), idToken, "sub");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal, principal.getAuthorities(), "google");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setNew(false);
        request.setSession(session);
        String anonymousSessionId = session.getId();
        MockHttpServletResponse response = new MockHttpServletResponse();

        socialLoginAuthenticationHandler.onAuthenticationSuccess(request, response, authentication);

        assertThat(session.getAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE))
                .isInstanceOf(Long.class);
        assertThat(session.getId()).isNotEqualTo(anonymousSessionId);
        assertThat(response.getRedirectedUrl()).isEqualTo("/auth/callback?newUser=true");
    }

    @DisplayName("네이버 로그인은 미검증 프로필 이메일을 회원 이메일로 저장하지 않는다")
    @Test
    void socialLogin_doesNotPersistNaverProfileEmail() throws Exception {
        Map<String, Object> attributes = Map.of(
                "id", "naver-account-id",
                "email", "unverified@naver.com",
                "name", "네이버 사용자");
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "id");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal, principal.getAuthorities(), "naver");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setNew(false);
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();

        socialLoginAuthenticationHandler.onAuthenticationSuccess(request, response, authentication);

        Long userId = (Long) session.getAttribute(
                CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        var user = userReader.findById(userId).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(user.getEmail()).isNull();
            softly.assertThat(user.getEmailEnc()).isNull();
            softly.assertThat(user.getEmailHmac()).isNull();
            softly.assertThat(user.getName()).isEqualTo("네이버 사용자");
            softly.assertThat(response.getRedirectedUrl()).isEqualTo("/auth/callback?newUser=true");
        });
    }

    @DisplayName("기존 세션으로 로그인하면 세션 ID가 교체된다")
    @Test
    void login_rotatesExistingSessionId() throws Exception {
        var signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifiedSignupRequest(
                                "rotate@example.com", "세션교체", "010-7777-8888"))))
                .andExpect(status().isCreated())
                .andReturn();
        var existingSession = signupResult.getResponse().getCookie("HG_SESSION");

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .cookie(existingSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CustomerLoginRequest("rotate@example.com", "password123"))))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(loginResult.getResponse().getCookie("HG_SESSION"))
                .isNotNull()
                .extracting(Cookie::getValue)
                .isNotEqualTo(existingSession.getValue());
    }

    private SignupRequest verifiedSignupRequest(String email, String name, String phone) throws Exception {
        return new SignupRequest(email, "password123", name, phone, issueVerificationCode(phone));
    }

    private String issueVerificationCode(String phone) throws Exception {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendVerificationRequest(normalizedPhone))))
                .andExpect(status().isOk());
        return phoneVerificationReader.findLatestUnverifiedCode(normalizedPhone)
                .orElseThrow(() -> new AssertionError("No verification code found"))
                .getCode();
    }
}
