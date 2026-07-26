package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminAuthService implements AdminAuthUseCase {

    private final AdminUserPort adminUserRepository;
    private final AdminSessionPort sessionPort;
    private final AdminAuthenticationTransactionService authenticationService;
    private final AdminAuthAuditService auditService;

    public DefaultAdminAuthService(
            AdminUserPort adminUserRepository,
            AdminSessionPort sessionPort,
            AdminAuthenticationTransactionService authenticationService,
            AdminAuthAuditService auditService) {
        this.adminUserRepository = adminUserRepository;
        this.sessionPort = sessionPort;
        this.authenticationService = authenticationService;
        this.auditService = auditService;
    }

    @Override
    public LoginResult login(String username, String rawPassword) {
        var decision = authenticationService.authenticatePassword(username, rawPassword);
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
        boolean valid = adminUserRepository.findById(session.adminUserId())
                .filter(user -> user.getUsername().equals(session.username()))
                .filter(user -> user.getCredentialVersion() == session.credentialVersion())
                .isPresent();
        if (!valid) {
            sessionPort.remove(token);
            return Optional.empty();
        }
        return storedSession;
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
                decision.adminUserId(), decision.username(), decision.credentialVersion());
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
