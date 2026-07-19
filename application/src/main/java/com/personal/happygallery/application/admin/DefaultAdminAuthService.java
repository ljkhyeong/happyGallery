package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DefaultAdminAuthService implements AdminAuthUseCase {

    private final AdminUserPort adminUserRepository;
    private final AdminSessionPort sessionPort;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminAuthService(AdminUserPort adminUserRepository, AdminSessionPort sessionPort,
                                   PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.sessionPort = sessionPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String login(String username, String rawPassword) {
        return adminUserRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(user -> sessionPort.create(
                        user.getId(), user.getUsername(), user.getCredentialVersion()))
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS,
                        "아이디 또는 비밀번호가 올바르지 않습니다."));
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
}
