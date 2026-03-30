package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort;
import com.personal.happygallery.app.admin.port.out.AdminSessionPort.AdminSession;
import com.personal.happygallery.app.web.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.app.admin.port.out.AdminUserPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public String login(String username, String rawPassword) {
        return adminUserRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(user -> sessionPort.create(user.getId(), user.getUsername()))
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS,
                        "아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    public Optional<AdminSession> validateToken(String token) {
        return sessionPort.validate(token);
    }

    public void logout(String token) {
        sessionPort.remove(token);
    }
}
