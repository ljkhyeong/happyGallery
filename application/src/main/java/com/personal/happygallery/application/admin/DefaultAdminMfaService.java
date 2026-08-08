package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.AdminAuthenticationMethod;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase;
import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryAttemptGuard;
import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAdminMfaService implements AdminMfaUseCase {

    private static final int RECOVERY_CODE_COUNT = 10;

    private final AdminUserPort adminUserPort;
    private final AdminMfaRecoveryCodePort recoveryCodePort;
    private final AdminTotpPort totpPort;
    private final AdminMfaCodeVerifier mfaCodeVerifier;
    private final AdminAuthAuditService auditService;
    private final AdminMfaRecoveryAttemptGuard recoveryAttemptGuard;
    private final AdminMfaRecoveryTransactionService recoveryTransactionService;
    private final AdminMfaResetService mfaResetService;
    private final FieldEncryptor fieldEncryptor;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DefaultAdminMfaService(
            AdminUserPort adminUserPort,
            AdminMfaRecoveryCodePort recoveryCodePort,
            AdminTotpPort totpPort,
            AdminMfaCodeVerifier mfaCodeVerifier,
            AdminAuthAuditService auditService,
            AdminMfaRecoveryAttemptGuard recoveryAttemptGuard,
            AdminMfaRecoveryTransactionService recoveryTransactionService,
            AdminMfaResetService mfaResetService,
            FieldEncryptor fieldEncryptor,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.adminUserPort = adminUserPort;
        this.recoveryCodePort = recoveryCodePort;
        this.totpPort = totpPort;
        this.mfaCodeVerifier = mfaCodeVerifier;
        this.auditService = auditService;
        this.recoveryAttemptGuard = recoveryAttemptGuard;
        this.recoveryTransactionService = recoveryTransactionService;
        this.mfaResetService = mfaResetService;
        this.fieldEncryptor = fieldEncryptor;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MfaStatus getStatus(Long adminUserId) {
        AdminUser admin = adminUserPort.findById(adminUserId)
                .orElseThrow(NotFoundException.supplier("관리자"));
        return new MfaStatus(
                admin.isMfaEnabled(),
                admin.hasPendingMfaEnrollment(),
                admin.isMfaEnabled()
                        ? recoveryCodePort.countUnusedByAdminUserId(adminUserId)
                        : 0);
    }

    @Override
    @Transactional
    public MfaEnrollment beginEnrollment(Long adminUserId) {
        AdminUser admin = adminUserPort.findByIdForUpdate(adminUserId)
                .orElseThrow(NotFoundException.supplier("관리자"));
        if (admin.isMfaEnabled()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "MFA가 이미 활성화되어 있습니다.");
        }

        AdminTotpPort.Enrollment enrollment = totpPort.generateEnrollment(admin.getUsername());
        admin.beginMfaEnrollment(fieldEncryptor.encrypt(enrollment.secret()));
        adminUserPort.save(admin);
        return new MfaEnrollment(enrollment.secret(), enrollment.provisioningUri());
    }

    @Override
    @Transactional
    public RecoveryCodes confirmEnrollment(Long adminUserId, String code) {
        AdminUser admin = adminUserPort.findByIdForUpdate(adminUserId)
                .orElseThrow(NotFoundException.supplier("관리자"));
        if (!admin.hasPendingMfaEnrollment()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "확인할 MFA 등록 정보가 없습니다.");
        }
        if (!mfaCodeVerifier.verifyAndUseTotp(
                admin, fieldEncryptor.decrypt(admin.getTotpSecretEnc()), code)) {
            throw invalidCredentials();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<String> recoveryCodes = totpPort.generateRecoveryCodes(RECOVERY_CODE_COUNT);
        recoveryCodePort.deleteByAdminUserId(adminUserId);
        for (String rawCode : recoveryCodes) {
            recoveryCodePort.save(new AdminMfaRecoveryCode(
                    adminUserId,
                    passwordEncoder.encode(rawCode),
                    now));
        }

        long invalidatedCredentialVersion = admin.enableMfa();
        adminUserPort.save(admin);
        auditService.record(admin.getId(), admin.getUsername(), AdminAuthOutcome.MFA_ENABLED);
        eventPublisher.publishEvent(new AdminCredentialsChangedEvent(
                admin.getId(), invalidatedCredentialVersion));
        return new RecoveryCodes(recoveryCodes);
    }

    @Override
    @Transactional
    public void disable(Long adminUserId, String currentPassword, String code) {
        AdminUser admin = adminUserPort.findByIdForUpdate(adminUserId)
                .orElseThrow(NotFoundException.supplier("관리자"));
        if (!admin.isMfaEnabled()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "MFA가 활성화되어 있지 않습니다.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean passwordMatches =
                passwordEncoder.matches(currentPassword, admin.getPasswordHash());
        AdminMfaCodeVerifier.Verification verification = mfaCodeVerifier.verifyAndConsume(
                admin,
                fieldEncryptor.decrypt(admin.getTotpSecretEnc()),
                code,
                now);
        if (!passwordMatches || verification == AdminMfaCodeVerifier.Verification.INVALID) {
            throw invalidCredentials();
        }

        mfaResetService.resetAfterDisable(admin);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void recover(
            Long adminUserId,
            String currentPassword,
            AdminAuthenticationMethod authenticationMethod) {
        if (authenticationMethod != AdminAuthenticationMethod.RECOVERY_CODE) {
            throw new HappyGalleryException(
                    ErrorCode.FORBIDDEN,
                    "복구 코드로 로그인한 관리자 세션이 필요한 작업입니다.");
        }

        recoveryAttemptGuard.check(adminUserId);
        recoveryTransactionService.recover(adminUserId, currentPassword);
    }

    private static HappyGalleryException invalidCredentials() {
        return new HappyGalleryException(
                ErrorCode.INVALID_CREDENTIALS, "관리자 인증 정보가 올바르지 않습니다.");
    }
}
