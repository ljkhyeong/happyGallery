package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.admin.port.out.AdminLoginSnapshot;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAdminAuthService implements AdminAuthUseCase {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final AdminUserPort adminUserRepository;
    private final AdminSessionPort sessionPort;
    private final AdminAuthenticationTransactionService authenticationService;
    private final AdminAuthAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminAuthService(
            AdminUserPort adminUserRepository,
            AdminSessionPort sessionPort,
            AdminAuthenticationTransactionService authenticationService,
            AdminAuthAuditService auditService,
            PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.sessionPort = sessionPort;
        this.authenticationService = authenticationService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public LoginResult login(String username, String rawPassword) {
        AdminLoginSnapshot snapshot = adminUserRepository
                .findLoginSnapshotByUsername(username)
                .orElse(null);
        String passwordHash = snapshot == null
                ? DUMMY_PASSWORD_HASH
                : snapshot.passwordHash();
        if (!passwordEncoder.matches(rawPassword, passwordHash) || snapshot == null) {
            auditService.record(
                    snapshot == null ? null : snapshot.adminUserId(),
                    username,
                    AdminAuthOutcome.LOGIN_FAILED);
            return completeLogin(
                    AdminAuthenticationTransactionService.AuthenticationDecision.rejected());
        }

        var decision = authenticationService.authenticatePassword(snapshot, rawPassword);
        if (decision.requiresMfa()) {
            return LoginResult.mfaRequired(decision.challengeToken());
        }
        return completeLogin(decision);
    }

    @Override
    public LoginResult verifyMfa(String challengeToken, String code) {
        return completeLogin(authenticationService.authenticateMfa(challengeToken, code));
    }

    @Override
    public Optional<AdminSession> validateToken(String token) {
        Optional<AdminSession> storedSession = sessionPort.validate(token);
        if (storedSession.isEmpty()) {
            return Optional.empty();
        }

        AdminSession session = storedSession.get();
        return adminUserRepository.findById(session.adminUserId())
                .filter(user -> user.getUsername().equals(session.username()))
                .filter(user -> user.getCredentialVersion() == session.credentialVersion())
                .map(user -> new AdminSession(
                        session.adminUserId(),
                        session.username(),
                        session.credentialVersion(),
                        user.isMfaEnabled(),
                        session.createdAt()))
                .or(() -> {
                    sessionPort.remove(token);
                    return Optional.empty();
                });
    }

    @Override
    public void logout(String token) {
        sessionPort.remove(token);
    }

    private LoginResult completeLogin(
            AdminAuthenticationTransactionService.AuthenticationDecision decision) {
        if (!decision.accepted()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_CREDENTIALS, "관리자 인증 정보가 올바르지 않습니다.");
        }

        String token = sessionPort.create(
                decision.adminUserId(),
                decision.username(),
                decision.credentialVersion(),
                decision.mfaEnabled());
        try {
            auditService.record(
                    decision.adminUserId(), decision.username(), AdminAuthOutcome.LOGIN_SUCCEEDED);
        } catch (RuntimeException exception) {
            try {
                sessionPort.remove(token);
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
        return LoginResult.authenticated(token);
    }
}
