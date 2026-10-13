package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class AdminMfaCodeVerifier {

    private static final Pattern TOTP_CODE = Pattern.compile("\\d{6}");
    private static final Pattern RECOVERY_CODE =
            Pattern.compile("[a-z0-9]{4}(?:-[a-z0-9]{4}){3}");

    private final AdminMfaRecoveryCodePort recoveryCodePort;
    private final AdminTotpPort totpPort;
    private final PasswordEncoder passwordEncoder;
    private final FieldEncryptor fieldEncryptor;

    AdminMfaCodeVerifier(AdminMfaRecoveryCodePort recoveryCodePort,
                         AdminTotpPort totpPort,
                         PasswordEncoder passwordEncoder,
                         FieldEncryptor fieldEncryptor) {
        this.recoveryCodePort = recoveryCodePort;
        this.totpPort = totpPort;
        this.passwordEncoder = passwordEncoder;
        this.fieldEncryptor = fieldEncryptor;
    }

    Verification verifyAndConsume(AdminUser admin, String rawCode, LocalDateTime now) {
        String secret = totpSecret(admin);
        String code = rawCode.trim().toLowerCase(Locale.ROOT);
        if (matchesTotp(admin, secret, code)) {
            return Verification.TOTP;
        }
        if (!RECOVERY_CODE.matcher(code).matches()) {
            return Verification.INVALID;
        }
        for (AdminMfaRecoveryCode stored :
                recoveryCodePort.findUnusedByAdminUserIdForUpdate(admin.getId())) {
            if (passwordEncoder.matches(code, stored.getCodeHash())) {
                stored.use(now);
                recoveryCodePort.save(stored);
                return Verification.RECOVERY_CODE;
            }
        }
        return Verification.INVALID;
    }

    boolean verifyAndUseTotp(AdminUser admin, String rawCode) {
        return matchesTotp(admin, totpSecret(admin), rawCode);
    }

    private boolean matchesTotp(AdminUser admin, String secret, String rawCode) {
        String code = rawCode.trim();
        if (!TOTP_CODE.matcher(code).matches()) {
            return false;
        }
        OptionalLong timeStep = totpPort.findMatchingTimeStep(secret, code);
        return timeStep.isPresent() && admin.acceptTotpStep(timeStep.getAsLong());
    }

    /** 코드 형식과 관계없이 먼저 복호화해 비밀값 오류를 검증 결과보다 앞서 드러낸다. */
    private String totpSecret(AdminUser admin) {
        return fieldEncryptor.decrypt(admin.getTotpSecretEnc());
    }

    enum Verification {
        TOTP,
        RECOVERY_CODE,
        INVALID
    }
}
