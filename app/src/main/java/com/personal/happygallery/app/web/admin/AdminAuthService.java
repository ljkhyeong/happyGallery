package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.infra.admin.AdminUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final AdminSessionStore sessionStore;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthService(AdminUserRepository adminUserRepository, AdminSessionStore sessionStore) {
        this.adminUserRepository = adminUserRepository;
        this.sessionStore = sessionStore;
    }

    public Optional<String> login(String username, String rawPassword) {
        return adminUserRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()))
                .map(user -> sessionStore.create(user.getId(), user.getUsername()));
    }

    public Optional<AdminSessionStore.Session> validateToken(String token) {
        return sessionStore.validate(token);
    }

    public void logout(String token) {
        sessionStore.remove(token);
    }
}
