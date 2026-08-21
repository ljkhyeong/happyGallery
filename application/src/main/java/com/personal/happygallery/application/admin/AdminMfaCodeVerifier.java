package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.application.admin.port.out.AdminTotpPort;
import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import com.personal.happygallery.domain.admin.AdminUser;
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

    AdminMfaCodeVerifier(AdminMfaRecoveryCodePort recoveryCodePort,
                         AdminTotpPort totpPort,
                         PasswordEncoder passwordEncoder) {
        this.recoveryCodePort = recoveryCodePort;
        this.totpPort = totpPort;
        this.passwordEncoder = passwordEncoder;
    }

    Verification verifyAndConsume(
            AdminUser admin,
            String secret,
            String rawCode,
            LocalDateTime now) {
        String code = rawCode.trim().toLowerCase(Locale.ROOT);
        if (verifyAndUseTotp(admin, secret, code)) {
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

    boolean verifyAndUseTotp(AdminUser admin, String secret, String rawCode) {
        String code = rawCode.trim();
        if (!TOTP_CODE.matcher(code).matches()) {
            return false;
        }
        OptionalLong timeStep = totpPort.findMatchingTimeStep(secret, code);
        return timeStep.isPresent() && admin.acceptTotpStep(timeStep.getAsLong());
    }

    enum Verification {
        TOTP,
        RECOVERY_CODE,
        INVALID
    }
}
