package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SignupRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialLoginRequest;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
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

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearUsers();
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
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "test@example.com", "password123", "테스트", "010-1234-5678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.phone").value("01012345678"))
                .andExpect(jsonPath("$.phoneVerified").value(false))
                .andExpect(jsonPath("$.provider").doesNotExist())
                .andExpect(cookie().exists("HG_SESSION"))
                .andExpect(cookie().httpOnly("HG_SESSION", true));
    }

    @DisplayName("중복 이메일로 회원가입하면 409를 반환한다")
    @Test
    void signup_duplicateEmail_conflict() throws Exception {
        String firstBody = objectMapper.writeValueAsString(new SignupRequest(
                "dup@example.com", "password123", "테스트", "010-0000-0000"));
        String duplicateBody = objectMapper.writeValueAsString(new SignupRequest(
                "DUP@EXAMPLE.COM", "password123", "테스트", "010-0000-0000"));

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

    @DisplayName("로그인 성공 시 사용자 정보와 세션 쿠키를 받는다")
    @Test
    void login_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "login@example.com", "password123", "로그인", "010-1111-2222"))))
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
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "wrong@example.com", "password123", "테스트", "010-3333-4444"))))
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
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "logout@example.com", "password123", "로그아웃", "010-9999-0000"))))
                .andExpect(status().isCreated())
                .andReturn();

        var sessionCookie = signupResult.getResponse().getCookie("HG_SESSION");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("HG_SESSION", 0));
    }

    @DisplayName("소셜 로그인 state가 세션과 다르면 로그인을 거절한다")
    @Test
    void socialLogin_rejectsMismatchedState() throws Exception {
        String redirectUri = "https://happygallery.example/auth/callback/naver";
        var authorizationResult = mockMvc.perform(get("/api/v1/auth/social/naver/url")
                        .param("redirectUri", redirectUri))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.url").value(startsWith(
                        redirectUri + "?code=fake-naver-code&state=")))
                .andExpect(cookie().exists("HG_SESSION"))
                .andReturn();

        var sessionCookie = authorizationResult.getResponse().getCookie("HG_SESSION");

        mockMvc.perform(post("/api/v1/auth/social/naver")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SocialLoginRequest("oauth-code", redirectUri, "wrong-state"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SOCIAL_LOGIN_FAILED"));
    }

    @DisplayName("state를 보내지 않는 기존 Google 콜백도 전환 배포 중에는 로그인된다")
    @Test
    void googleSocialLogin_acceptsLegacyRequestWithoutState() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social/google")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SocialLoginRequest(
                                "legacy-google-code",
                                "https://happygallery.example/auth/callback/google",
                                null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("social-test@example.com"))
                .andExpect(cookie().exists("HG_SESSION"));
    }

    @DisplayName("기존 세션으로 로그인하면 세션 ID가 교체된다")
    @Test
    void login_rotatesExistingSessionId() throws Exception {
        var signupResult = mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupRequest(
                                "rotate@example.com", "password123", "세션교체", "010-7777-8888"))))
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
}
