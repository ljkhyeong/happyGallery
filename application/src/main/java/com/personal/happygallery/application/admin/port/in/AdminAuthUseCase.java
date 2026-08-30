package com.personal.happygallery.application.admin.port.in;

import com.personal.happygallery.application.admin.port.AdminSession;
import java.util.Optional;

public interface AdminAuthUseCase {

    LoginResult login(String username, String rawPassword);

    LoginResult verifyMfa(String challengeToken, String code);

    Optional<AdminSession> validateToken(String token);

    void logout(String token);

    enum LoginStatus {
        AUTHENTICATED,
        MFA_REQUIRED
    }

    record LoginResult(
            LoginStatus status,
            String token,
            String challengeToken
    ) {
        public static LoginResult authenticated(String token) {
            return new LoginResult(LoginStatus.AUTHENTICATED, token, null);
        }

        public static LoginResult mfaRequired(String challengeToken) {
            return new LoginResult(LoginStatus.MFA_REQUIRED, null, challengeToken);
        }
    }
}
