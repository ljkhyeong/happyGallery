package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaChallengePort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.admin.AdminMfaChallenge;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminAuthenticationTransactionService {

    private static final Duration MFA_CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final AdminUserPort adminUserPort;
    private final AdminMfaChallengePort challengePort;
    private final AdminMfaCodeVerifier mfaCodeVerifier;
    private final AdminAuthAuditService auditService;
    private final BlindIndexKeyRing blindIndexKeyRing;
    private final FieldEncryptor fieldEncryptor;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    AdminAuthenticationTransactionService(
            AdminUserPort adminUserPort,
            AdminMfaChallengePort challengePort,
            AdminMfaCodeVerifier mfaCodeVerifier,
            AdminAuthAuditService auditService,
            BlindIndexKeyRing blindIndexKeyRing,
            FieldEncryptor fieldEncryptor,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.adminUserPort = adminUserPort;
        this.challengePort = challengePort;
        this.mfaCodeVerifier = mfaCodeVerifier;
        this.auditService = auditService;
        this.blindIndexKeyRing = blindIndexKeyRing;
        this.fieldEncryptor = fieldEncryptor;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AuthenticationDecision authenticatePassword(String username, String rawPassword) {
        AdminUser admin = adminUserPort.findByUsernameForUpdate(username).orElse(null);
        if (admin == null) {
            passwordEncoder.matches(rawPassword, DUMMY_PASSWORD_HASH);
            auditService.record(null, username, AdminAuthOutcome.LOGIN_FAILED);
            return AuthenticationDecision.rejected();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        boolean passwordMatches = passwordEncoder.matches(rawPassword, admin.getPasswordHash());
        if (admin.isAuthenticationLocked(now)) {
            auditService.record(admin.getId(), username, AdminAuthOutcome.LOGIN_BLOCKED);
            return AuthenticationDecision.rejected();
        }
        if (!passwordMatches) {
            admin.recordFailedAuthentication(now);
            adminUserPort.save(admin);
            auditService.record(
                    admin.getId(),
                    username,
                    admin.isAuthenticationLocked(now)
                            ? AdminAuthOutcome.LOGIN_BLOCKED
                            : AdminAuthOutcome.LOGIN_FAILED);
            return AuthenticationDecision.rejected();
        }

        if (passwordEncoder.upgradeEncoding(admin.getPasswordHash())) {
            admin.upgradePasswordHash(passwordEncoder.encode(rawPassword));
            adminUserPort.save(admin);
        }
        if (!admin.isMfaEnabled()) {
            admin.authenticationSucceeded();
            adminUserPort.save(admin);
            return AuthenticationDecision.authenticated(admin);
        }

        challengePort.deleteByAdminUserId(admin.getId());
        String challengeToken = UUID.randomUUID().toString();
        challengePort.save(new AdminMfaChallenge(
                admin.getId(),
                blindIndexKeyRing.index(challengeToken),
                now.plus(MFA_CHALLENGE_TTL),
                now));
        auditService.record(admin.getId(), username, AdminAuthOutcome.MFA_REQUIRED);
        return AuthenticationDecision.mfaRequired(challengeToken);
    }

    @Transactional
    public AuthenticationDecision authenticateMfa(String challengeToken, String code) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<String> tokenHmacs = blindIndexKeyRing.indexCandidates(challengeToken);
        Long adminUserId = challengePort
                .findAdminUserIdByTokenHmacCandidates(tokenHmacs)
                .orElse(null);
        if (adminUserId == null) {
            auditService.record(null, challengeToken, AdminAuthOutcome.MFA_FAILED);
            return AuthenticationDecision.rejected();
        }

        AdminUser admin = adminUserPort.findByIdForUpdate(adminUserId).orElse(null);
        AdminMfaChallenge challenge = challengePort
                .findByTokenHmacCandidatesForUpdate(tokenHmacs)
                .orElse(null);
        if (admin == null
                || challenge == null
                || !challenge.isUsable(now)
                || !admin.isMfaEnabled()) {
            auditService.record(
                    admin == null ? null : admin.getId(),
                    challengeToken,
                    AdminAuthOutcome.MFA_FAILED);
            return AuthenticationDecision.rejected();
        }
        if (admin.isAuthenticationLocked(now)) {
            auditService.record(admin.getId(), admin.getUsername(), AdminAuthOutcome.LOGIN_BLOCKED);
            return AuthenticationDecision.rejected();
        }

        AdminMfaCodeVerifier.Verification verification = mfaCodeVerifier.verifyAndConsume(
                admin,
                fieldEncryptor.decrypt(admin.getTotpSecretEnc()),
                code,
                now);
        if (verification == AdminMfaCodeVerifier.Verification.INVALID) {
            admin.recordFailedAuthentication(now);
            adminUserPort.save(admin);
            auditService.record(
                    admin.getId(),
                    admin.getUsername(),
                    admin.isAuthenticationLocked(now)
                            ? AdminAuthOutcome.LOGIN_BLOCKED
                            : AdminAuthOutcome.MFA_FAILED);
            return AuthenticationDecision.rejected();
        }

        challenge.consume(now);
        challengePort.save(challenge);
        admin.authenticationSucceeded();
        adminUserPort.save(admin);
        if (verification == AdminMfaCodeVerifier.Verification.RECOVERY_CODE) {
            auditService.record(
                    admin.getId(), admin.getUsername(), AdminAuthOutcome.RECOVERY_CODE_USED);
        }
        return AuthenticationDecision.authenticated(admin);
    }

    record AuthenticationDecision(
            boolean accepted,
            Long adminUserId,
            String username,
            long credentialVersion,
            boolean mfaEnabled,
            String challengeToken
    ) {
        static AuthenticationDecision authenticated(AdminUser admin) {
            return new AuthenticationDecision(
                    true,
                    admin.getId(),
                    admin.getUsername(),
                    admin.getCredentialVersion(),
                    admin.isMfaEnabled(),
                    null);
        }

        static AuthenticationDecision mfaRequired(String challengeToken) {
            return new AuthenticationDecision(false, null, null, 0, false, challengeToken);
        }

        static AuthenticationDecision rejected() {
            return new AuthenticationDecision(false, null, null, 0, false, null);
        }

        boolean requiresMfa() {
            return challengeToken != null;
        }
    }
}
