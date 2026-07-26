package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaCodeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaVerificationRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.LoginRequest;
import com.personal.happygallery.adapter.out.persistence.admin.AdminAuthHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.admin.AdminMfaChallengeRepository;
import com.personal.happygallery.adapter.out.persistence.admin.AdminMfaRecoveryCodeRepository;
import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.admin.AdminMfaChallenge;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminLoginUseCaseIT {

    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin1234";
    private static final String MFA_SECRET = "JBSWY3DPEHPK3PXP";
    private static final List<String> RECOVERY_CODES = List.of(
            "aaaa-bbbb-cccc-0001",
            "aaaa-bbbb-cccc-0002",
            "aaaa-bbbb-cccc-0003",
            "aaaa-bbbb-cccc-0004",
            "aaaa-bbbb-cccc-0005",
            "aaaa-bbbb-cccc-0006",
            "aaaa-bbbb-cccc-0007",
            "aaaa-bbbb-cccc-0008",
            "aaaa-bbbb-cccc-0009",
            "aaaa-bbbb-cccc-0010");

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired AdminUserPort adminUserPort;
    @Autowired AdminAuthHistoryRepository historyRepository;
    @Autowired AdminMfaChallengeRepository challengeRepository;
    @MockitoSpyBean AdminMfaRecoveryCodeRepository recoveryCodeRepository;
    @Autowired BlindIndexKeyRing blindIndexKeyRing;
    @Autowired FieldEncryptor fieldEncryptor;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @MockitoBean AdminTotpPort totpPort;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        adminUserPort.save(new AdminUser(USERNAME, passwordEncoder.encode(PASSWORD)));
        when(totpPort.generateEnrollment(USERNAME))
                .thenReturn(new AdminTotpPort.Enrollment(
                        MFA_SECRET,
                        "otpauth://totp/%ED%95%B4%ED%94%BC%EA%B0%A4%EB%9F%AC%EB%A6%AC:admin"
                                + "?secret=" + MFA_SECRET));
        when(totpPort.findMatchingTimeStep(anyString(), eq("123456")))
                .thenReturn(OptionalLong.of(100));
        when(totpPort.findMatchingTimeStep(anyString(), eq("654321")))
                .thenReturn(OptionalLong.of(101));
        when(totpPort.generateRecoveryCodes(RECOVERY_CODES.size())).thenReturn(RECOVERY_CODES);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearAdminUsers();
    }

    @DisplayName("관리자 계정으로 로그인할 수 있다")
    @Test
    void login_defaultAdmin_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.challengeToken").isEmpty());

        assertThat(historyRepository.findAllByOrderByIdAsc())
                .extracting(history -> history.getOutcome())
                .containsExactly(AdminAuthOutcome.LOGIN_SUCCEEDED);
    }

    @DisplayName("로그인 실패가 5회 누적되면 계정을 잠그고 존재 여부와 잠금 상태를 같은 응답으로 숨긴다")
    @Test
    void repeatedFailure_locksAccount_withoutRevealingAccountState() throws Exception {
        for (int attempt = 0; attempt < AdminUser.MAX_FAILED_LOGIN_ATTEMPTS; attempt++) {
            assertInvalidCredentials(USERNAME, "wrong-password");
        }
        assertInvalidCredentials(USERNAME, PASSWORD);
        assertInvalidCredentials("missing-admin", "wrong-password");

        AdminUser stored = adminUserPort.findByUsername(USERNAME).orElseThrow();
        assertThat(stored.getFailedLoginAttempts()).isEqualTo(AdminUser.MAX_FAILED_LOGIN_ATTEMPTS);
        assertThat(stored.getLockedUntil()).isNotNull();
        assertThat(historyRepository.findAllByOrderByIdAsc())
                .extracting(history -> history.getOutcome())
                .contains(
                        AdminAuthOutcome.LOGIN_FAILED,
                        AdminAuthOutcome.LOGIN_BLOCKED);
    }

    @DisplayName("MFA 등록 후에는 TOTP 시간 구간과 복구 코드를 한 번만 허용한다")
    @Test
    void mfaEnrollment_requiresSecondStep_andConsumesRecoveryCodeOnce() throws Exception {
        assertInvalidCredentials(USERNAME, "wrong-password");
        String enrollmentToken = loginAndGetToken();
        assertThat(adminUserPort.findByUsername(USERNAME).orElseThrow().getFailedLoginAttempts())
                .isZero();

        JsonNode enrollment = responseBody(mockMvc.perform(
                        post("/api/v1/admin/auth/mfa/enrollment")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + enrollmentToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.secret").value(MFA_SECRET))
                .andReturn());
        assertThat(enrollment.get("provisioningUri").asText()).startsWith("otpauth://totp/");

        AdminUser pending = adminUserPort.findByUsername(USERNAME).orElseThrow();
        assertThat(pending.getTotpSecretEnc()).isNotEqualTo(MFA_SECRET);
        assertThat(fieldEncryptor.decrypt(pending.getTotpSecretEnc())).isEqualTo(MFA_SECRET);

        JsonNode recoveryResponse = responseBody(mockMvc.perform(
                        post("/api/v1/admin/auth/mfa/enrollment/confirm")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + enrollmentToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        new AdminMfaCodeRequest("123456"))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.recoveryCodes.length()").value(RECOVERY_CODES.size()))
                .andReturn());
        assertThat(recoveryResponse.get("recoveryCodes").get(0).asText())
                .isEqualTo(RECOVERY_CODES.get(0));
        assertThat(recoveryCodeRepository.findAll())
                .allSatisfy(stored -> assertThat(stored.getCodeHash())
                        .isNotIn(RECOVERY_CODES));
        assertProtectedRequestRejected(enrollmentToken);

        String challenge = loginAndGetChallenge();
        assertThat(challengeRepository.findAll())
                .singleElement()
                .satisfies(stored -> assertThat(stored.getTokenHmac()).isNotEqualTo(challenge));
        clearInvocations(recoveryCodeRepository);
        assertInvalidMfa(challenge, "000000");
        verify(recoveryCodeRepository, never())
                .findUnusedByAdminUserIdForUpdate(adminUserPort.findByUsername(USERNAME)
                        .orElseThrow()
                        .getId());
        assertThat(adminUserPort.findByUsername(USERNAME).orElseThrow().getFailedLoginAttempts())
                .isEqualTo(1);
        String authenticatedToken = verifyMfaAndGetToken(challenge, "654321");
        assertThat(adminUserPort.findByUsername(USERNAME).orElseThrow().getFailedLoginAttempts())
                .isZero();
        assertProtectedRequestAccepted(authenticatedToken);
        assertInvalidMfa(challenge, "654321");

        String totpReplayChallenge = loginAndGetChallenge();
        assertInvalidMfa(totpReplayChallenge, "654321");
        assertThat(adminUserPort.findByUsername(USERNAME)
                .orElseThrow()
                .getLastAcceptedTotpStep()).isEqualTo(101);
        String recoveryChallenge = loginAndGetChallenge();
        clearInvocations(totpPort);
        verifyMfaAndGetToken(recoveryChallenge, RECOVERY_CODES.get(0));
        verify(totpPort, never())
                .findMatchingTimeStep(anyString(), eq(RECOVERY_CODES.get(0)));
        String replayChallenge = loginAndGetChallenge();
        assertInvalidMfa(replayChallenge, RECOVERY_CODES.get(0));
        assertThat(historyRepository.findAllByOrderByIdAsc())
                .extracting(history -> history.getOutcome())
                .contains(
                        AdminAuthOutcome.MFA_ENABLED,
                        AdminAuthOutcome.MFA_REQUIRED,
                        AdminAuthOutcome.RECOVERY_CODE_USED,
                        AdminAuthOutcome.MFA_FAILED);
    }

    @DisplayName("서로 다른 MFA challenge가 같은 TOTP 시간 구간을 동시에 검증해도 한 요청만 성공한다")
    @Test
    void concurrentMfaVerification_sameTimeStep_authenticatesOnce() throws Exception {
        String enrollmentToken = loginAndGetToken();
        mockMvc.perform(post("/api/v1/admin/auth/mfa/enrollment")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + enrollmentToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/auth/mfa/enrollment/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + enrollmentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminMfaCodeRequest("123456"))))
                .andExpect(status().isOk());

        AdminUser admin = adminUserPort.findByUsername(USERNAME).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        String firstChallenge = "concurrent-mfa-challenge-1";
        String secondChallenge = "concurrent-mfa-challenge-2";
        challengeRepository.save(new AdminMfaChallenge(
                admin.getId(),
                blindIndexKeyRing.index(firstChallenge),
                now.plusMinutes(5),
                now));
        challengeRepository.save(new AdminMfaChallenge(
                admin.getId(),
                blindIndexKeyRing.index(secondChallenge),
                now.plusMinutes(5),
                now));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<MvcResult> results;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> first = executor.submit(
                    () -> verifyMfaAfter(ready, start, firstChallenge, "654321"));
            Future<MvcResult> second = executor.submit(
                    () -> verifyMfaAfter(ready, start, secondChallenge, "654321"));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            results = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));
        }

        assertThat(results)
                .extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(200, 401);
        MvcResult succeeded = results.stream()
                .filter(result -> result.getResponse().getStatus() == 200)
                .findFirst()
                .orElseThrow();
        MvcResult rejected = results.stream()
                .filter(result -> result.getResponse().getStatus() == 401)
                .findFirst()
                .orElseThrow();
        JsonNode succeededBody = responseBody(succeeded);
        JsonNode rejectedBody = responseBody(rejected);

        assertSoftly(softly -> {
            softly.assertThat(succeededBody.get("status").asText())
                    .isEqualTo("AUTHENTICATED");
            softly.assertThat(rejectedBody.get("code").asText())
                    .isEqualTo("INVALID_CREDENTIALS");
            softly.assertThat(adminUserPort.findByUsername(USERNAME).orElseThrow()
                            .getLastAcceptedTotpStep())
                    .isEqualTo(101);
            softly.assertThat(challengeRepository.findAll())
                    .filteredOn(challenge -> challenge.getConsumedAt() != null)
                    .hasSize(1);
        });
    }

    private String loginAndGetToken() throws Exception {
        JsonNode body = responseBody(mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andReturn());
        return body.get("token").asText();
    }

    private String loginAndGetChallenge() throws Exception {
        JsonNode body = responseBody(mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MFA_REQUIRED"))
                .andExpect(jsonPath("$.token").isEmpty())
                .andReturn());
        return body.get("challengeToken").asText();
    }

    private String verifyMfaAndGetToken(String challenge, String code) throws Exception {
        JsonNode body = responseBody(mockMvc.perform(post("/api/v1/admin/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminMfaVerificationRequest(challenge, code))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andReturn());
        return body.get("token").asText();
    }

    private MvcResult verifyMfaAfter(
            CountDownLatch ready,
            CountDownLatch start,
            String challenge,
            String code) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("동시 MFA 검증 시작 신호를 기다리지 못했습니다.");
        }
        return mockMvc.perform(post("/api/v1/admin/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminMfaVerificationRequest(challenge, code))))
                .andReturn();
    }

    private void assertInvalidCredentials(String username, String password) throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("관리자 인증 정보가 올바르지 않습니다."));
    }

    private void assertInvalidMfa(String challenge, String code) throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/mfa/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminMfaVerificationRequest(challenge, code))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    private void assertProtectedRequestAccepted(String token) throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void assertProtectedRequestRejected(String token) throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode responseBody(
            org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
