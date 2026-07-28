package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.security.customer.CustomerAuthenticationFilter;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerStepUpAuthenticationStore;
import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.adapter.out.persistence.user.UserRepository;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase;
import com.personal.happygallery.application.customer.port.in.CustomerAccountLifecycleUseCase.WithdrawCommand;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.TestFixtures;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class CustomerAccountLifecycleUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired UserStorePort userStore;
    @Autowired UserReaderPort userReader;
    @Autowired UserRepository userRepository;
    @Autowired SocialAccountStorePort socialAccountStore;
    @Autowired SocialAccountRepository socialAccountRepository;
    @Autowired PassPurchaseStorePort passPurchaseStore;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired CustomerAccountLifecycleUseCase accountLifecycle;
    @Autowired CustomerStepUpAuthenticationStore stepUpAuthenticationStore;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearPassData();
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("회원 탈퇴는 로그인 자격과 개인정보와 소셜 연결을 폐기한다")
    void withdrawAnonymizesAccountAndRemovesLoginMethods() throws Exception {
        User user = activeUser("withdraw@example.com", "01012345678");
        socialAccountStore.save(new SocialAccount(
                user.getId(), SocialProvider.NAVER, "withdraw-naver-id"));
        String originalEmailHmac = user.getEmailHmac();

        mockMvc.perform(delete("/api/v1/me")
                        .with(csrf())
                        .session(customerSession(user)))
                .andExpect(status().isNoContent());

        User withdrawn = userRepository.findById(user.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(withdrawn.getWithdrawnAt()).isNotNull();
            softly.assertThat(withdrawn.getPasswordHash()).isNull();
            softly.assertThat(withdrawn.getPhoneEnc()).isNull();
            softly.assertThat(withdrawn.getPhoneHmac()).isNull();
            softly.assertThat(withdrawn.isPhoneVerified()).isFalse();
            softly.assertThat(withdrawn.getEmailHmac()).isNotEqualTo(originalEmailHmac);
            softly.assertThat(userReader.findById(user.getId())).isEmpty();
            softly.assertThat(socialAccountRepository.findAll()).isEmpty();
        });

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "withdraw@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("사용 가능한 8회권이 있으면 회원 탈퇴를 거절한다")
    void rejectWithdrawalWhilePassIsUsable() throws Exception {
        User user = activeUser("withdraw-blocked@example.com", "01087654321");
        passPurchaseStore.save(TestFixtures.passPurchase(
                user.getId(), LocalDateTime.of(2100, 1, 1, 0, 0), 320_000L));

        mockMvc.perform(delete("/api/v1/me")
                        .with(csrf())
                        .session(customerSession(user)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ACCOUNT_WITHDRAWAL_BLOCKED"));

        assertThat(userReader.findById(user.getId())).isPresent();
    }

    @Test
    @DisplayName("최근 본인 확인이 없는 세션은 회원 탈퇴를 실행할 수 없다")
    void rejectWithdrawalWithoutRecentAuthentication() throws Exception {
        User user = activeUser("withdraw-step-up@example.com", "01055556666");

        mockMvc.perform(delete("/api/v1/me")
                        .with(csrf())
                        .session(customerSessionWithoutStepUp(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REAUTHENTICATION_REQUIRED"));

        assertThat(userReader.findById(user.getId())).isPresent();
    }

    @Test
    @DisplayName("요청 후 자격 버전이 바뀌면 잠근 회원 기준으로 탈퇴를 거절한다")
    void rejectWithdrawalWithStaleCredentialVersion() {
        User user = activeUser("withdraw-stale@example.com", "01066667777");

        assertThatThrownBy(() -> accountLifecycle.withdraw(new WithdrawCommand(
                user.getId(), user.getCredentialVersion() + 1, true)))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(exception -> ((HappyGalleryException) exception).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(userReader.findById(user.getId())).isPresent();
    }

    private User activeUser(String email, String phone) {
        User user = new User(email, passwordEncoder.encode("password123"), "회원", phone);
        user.markPhoneVerified();
        return userStore.save(user);
    }

    private MockHttpSession customerSession(User user) {
        MockHttpSession session = customerSessionWithoutStepUp(user);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        stepUpAuthenticationStore.markVerified(
                request, user.getId(), user.getCredentialVersion());
        return session;
    }

    private MockHttpSession customerSessionWithoutStepUp(User user) {
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
