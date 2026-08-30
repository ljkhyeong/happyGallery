package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.AdminAuthenticationMethod;
import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryAttemptGuard;
import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DefaultAdminMfaServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 10, 30);

    private final AdminUserPort adminUserPort = mock(AdminUserPort.class);
    private final AdminMfaCodeVerifier mfaCodeVerifier = mock(AdminMfaCodeVerifier.class);
    private final AdminMfaRecoveryAttemptGuard recoveryAttemptGuard =
            mock(AdminMfaRecoveryAttemptGuard.class);
    private final AdminMfaRecoveryTransactionService recoveryTransactionService =
            mock(AdminMfaRecoveryTransactionService.class);
    private final AdminMfaResetService mfaResetService = mock(AdminMfaResetService.class);
    private final FieldEncryptor fieldEncryptor = mock(FieldEncryptor.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DefaultAdminMfaService service = new DefaultAdminMfaService(
            adminUserPort,
            mock(AdminMfaRecoveryCodePort.class),
            mock(AdminTotpPort.class),
            mfaCodeVerifier,
            mock(AdminAuthAuditService.class),
            recoveryAttemptGuard,
            recoveryTransactionService,
            mfaResetService,
            fieldEncryptor,
            passwordEncoder,
            mock(ApplicationEventPublisher.class),
            Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

    @DisplayName("관리자 MFA 비활성화는 검증 후 공통 초기화 서비스에 위임한다")
    @Test
    void disable_delegatesReset_afterCredentialsAreVerified() {
        AdminUser admin = mock(AdminUser.class);
        given(adminUserPort.findByIdForUpdate(42L)).willReturn(Optional.of(admin));
        given(admin.isMfaEnabled()).willReturn(true);
        given(admin.getPasswordHash()).willReturn("password-hash");
        given(admin.getTotpSecretEnc()).willReturn("encrypted-secret");
        given(passwordEncoder.matches("current-password", "password-hash")).willReturn(true);
        given(fieldEncryptor.decrypt("encrypted-secret")).willReturn("secret");
        given(mfaCodeVerifier.verifyAndConsume(admin, "secret", "123456", NOW))
                .willReturn(AdminMfaCodeVerifier.Verification.TOTP);

        service.disable(42L, "current-password", "123456");

        verify(mfaResetService).resetAfterDisable(admin);
    }

    @DisplayName("복구 코드가 아닌 세션은 관리자 MFA 복구 버킷을 소비하지 않는다")
    @Test
    void recover_nonRecoverySession_doesNotConsumeRateLimit() {
        assertThatThrownBy(() -> service.recover(
                42L, "current-password", AdminAuthenticationMethod.TOTP))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.FORBIDDEN));

        verifyNoInteractions(recoveryAttemptGuard, recoveryTransactionService);
    }

    @DisplayName("복구 코드 세션은 DB 복구 트랜잭션보다 먼저 관리자 버킷을 소비한다")
    @Test
    void recover_recoveryCodeSession_checksRateLimitBeforeTransaction() {
        service.recover(42L, "current-password", AdminAuthenticationMethod.RECOVERY_CODE);

        InOrder order = inOrder(recoveryAttemptGuard, recoveryTransactionService);
        order.verify(recoveryAttemptGuard).check(42L);
        order.verify(recoveryTransactionService).recover(42L, "current-password");
    }
}
