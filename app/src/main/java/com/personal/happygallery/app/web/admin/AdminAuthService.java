package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort;
import com.personal.happygallery.app.admin.port.out.AdminSessionPort.AdminSession;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.infra.admin.AdminUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final AdminSessionPort sessionPort;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(AdminUserRepository adminUserRepository, AdminSessionPort sessionPort) {
        this.adminUserRepository = adminUserRepository;
        this.sessionPort = sessionPort;
    }

    public Optional<String> login(String username, String rawPassword) {
        return adminUserRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(user -> sessionPort.create(user.getId(), user.getUsername()));
    }

    public Optional<AdminSession> validateToken(String token) {
        return sessionPort.validate(token);
    }

    public void logout(String token) {
        sessionPort.remove(token);
    }
}
