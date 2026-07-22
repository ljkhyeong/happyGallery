package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.ChangePasswordRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.ResetPasswordRequest;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.application.customer.port.out.CustomerSessionRevocationPort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class CustomerCredentialUseCaseIT {

    private static final String CURRENT_PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newPassword456";
    private static final String RESET_PASSWORD = "resetPassword789";

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired ObjectMapper objectMapper;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired GuestBookingUseCase guestBookingUseCase;
    @Autowired SocialAuthUseCase socialAuth;
    @Autowired MemberPhoneUpdateUseCase phoneUpdate;
    @Autowired UserReaderPort userReader;
    @Autowired CustomerSessionRevocationPort sessionRevocation;
    @Autowired FindByIndexNameSessionRepository<? extends Session> sessionRepository;
    @Autowired TestCleanupSupport cleanupSupport;

    private final Set<CredentialScope> credentialScopes = new HashSet<>();
    private MockMvc mockMvc;

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
        credentialScopes.forEach(scope ->
                sessionRevocation.revokeCredentialVersion(scope.userId(), scope.credentialVersion()));
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("비밀번호를 변경하면 모든 회원 세션을 폐기하고 새 비밀번호만 허용한다")
    void changePasswordRevokesAllSessions() throws Exception {
        String email = "credential-change@example.com";
        CustomerTestHelper customerHelper =
                new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader);
        Cookie firstSession = customerHelper.signupAndGetSessionCookie(email, "01012345678");
        User user = userReader.findByEmail(email).orElseThrow();
        long oldCredentialVersion = user.getCredentialVersion();
        credentialScopes.add(new CredentialScope(user.getId(), oldCredentialVersion));

        Cookie secondSession = login(email, CURRENT_PASSWORD);
        assertThat(sessionRepository.findByPrincipalName(
                principalName(user.getId(), oldCredentialVersion))).hasSize(2);

        mockMvc.perform(patch("/api/v1/me/password")
                        .with(csrf())
                        .cookie(firstSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))))
                .andExpect(status().isNoContent());

        assertThat(sessionRepository.findByPrincipalName(
                principalName(user.getId(), oldCredentialVersion))).isEmpty();
        mockMvc.perform(get("/api/v1/me").cookie(firstSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").cookie(secondSession))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CustomerLoginRequest(email, CURRENT_PASSWORD))))
                .andExpect(status().isUnauthorized());
        User changedUser = userReader.findById(user.getId()).orElseThrow();
        long newCredentialVersion = changedUser.getCredentialVersion();
        credentialScopes.add(new CredentialScope(user.getId(), newCredentialVersion));
        Cookie newSession = login(email, NEW_PASSWORD);

        sessionRevocation.revokeCredentialVersion(user.getId(), oldCredentialVersion);

        assertThat(sessionRepository.findByPrincipalName(
                principalName(user.getId(), newCredentialVersion))).hasSize(1);
        mockMvc.perform(get("/api/v1/me").cookie(newSession))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("소셜 전용 회원은 검증된 휴대폰으로 최초 로컬 비밀번호를 설정한다")
    void resetPasswordEstablishesLocalPasswordForSocialUser() throws Exception {
        String email = "credential-social@example.com";
        String phone = "01098765432";
        User socialUser = socialAuth.socialLogin(new SocialLoginCommand(
                SocialProvider.GOOGLE,
                "credential-social-provider-id",
                email,
                "소셜 회원")).user();
        credentialScopes.add(new CredentialScope(
                socialUser.getId(), socialUser.getCredentialVersion()));

        String registrationCode = guestBookingUseCase.sendVerificationCode(phone).getCode();
        phoneUpdate.update(socialUser.getId(), phone, registrationCode);
        String resetCode = guestBookingUseCase.sendVerificationCode(phone).getCode();

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(
                                email, phone, resetCode, NEW_PASSWORD))))
                .andExpect(status().isNoContent());

        User localUser = userReader.findById(socialUser.getId()).orElseThrow();
        long localCredentialVersion = localUser.getCredentialVersion();
        credentialScopes.add(new CredentialScope(socialUser.getId(), localCredentialVersion));
        Cookie firstSession = login(email, NEW_PASSWORD);
        Cookie secondSession = login(email, NEW_PASSWORD);
        assertThat(sessionRepository.findByPrincipalName(
                principalName(socialUser.getId(), localCredentialVersion))).hasSize(2);

        String nextResetCode = guestBookingUseCase.sendVerificationCode(phone).getCode();
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(
                                email, phone, nextResetCode, RESET_PASSWORD))))
                .andExpect(status().isNoContent());

        assertThat(sessionRepository.findByPrincipalName(
                principalName(socialUser.getId(), localCredentialVersion))).isEmpty();
        mockMvc.perform(get("/api/v1/me").cookie(firstSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").cookie(secondSession))
                .andExpect(status().isUnauthorized());
        User resetUser = userReader.findById(socialUser.getId()).orElseThrow();
        credentialScopes.add(new CredentialScope(
                socialUser.getId(), resetUser.getCredentialVersion()));
        login(email, RESET_PASSWORD);
    }

    @Test
    @DisplayName("소셜 계정 연결을 해제하면 자격 버전을 올리고 기존 회원 세션을 모두 폐기한다")
    void unlinkSocialAccountRevokesAllSessions() throws Exception {
        String email = "social-unlink@example.com";
        CustomerTestHelper customerHelper =
                new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader);
        Cookie firstSession = customerHelper.signupAndGetSessionCookie(email, "01056781234");
        User user = userReader.findByEmail(email).orElseThrow();
        long oldCredentialVersion = user.getCredentialVersion();
        credentialScopes.add(new CredentialScope(user.getId(), oldCredentialVersion));
        socialAuth.linkSocialAccount(new SocialAuthUseCase.SocialLinkCommand(
                user.getId(), oldCredentialVersion, SocialProvider.GOOGLE, "unlink-google-id"));
        Cookie secondSession = login(email, CURRENT_PASSWORD);

        mockMvc.perform(delete("/api/v1/me/social-accounts/google")
                        .with(csrf())
                        .cookie(firstSession))
                .andExpect(status().isNoContent());

        assertThat(sessionRepository.findByPrincipalName(
                principalName(user.getId(), oldCredentialVersion))).isEmpty();
        mockMvc.perform(get("/api/v1/me").cookie(firstSession))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me").cookie(secondSession))
                .andExpect(status().isUnauthorized());
        assertThat(userReader.findById(user.getId()).orElseThrow().getCredentialVersion())
                .isEqualTo(oldCredentialVersion + 1);
        assertThat(socialAuth.listLinkedProviders(user.getId())).isEmpty();
    }

    private Cookie login(String email, String password) throws Exception {
        Cookie sessionCookie = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CustomerLoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie("HG_SESSION");
        assertThat(sessionCookie).isNotNull();
        return sessionCookie;
    }

    private String principalName(Long userId, long credentialVersion) {
        return userId + ":" + credentialVersion;
    }

    private record CredentialScope(Long userId, long credentialVersion) {
    }
}
