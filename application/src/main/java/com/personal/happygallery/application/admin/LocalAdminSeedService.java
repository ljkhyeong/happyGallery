package com.personal.happygallery.application.admin;

import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * local 프로필에서만 기본 관리자 계정을 seed 한다.
 * 프로덕션에서는 별도 bootstrap 절차를 거쳐야 한다.
 */
@Service
@Profile("local")
public class LocalAdminSeedService {

    private static final Logger log = LoggerFactory.getLogger(LocalAdminSeedService.class);
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin1234";

    private final AdminUserPort adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public LocalAdminSeedService(AdminUserPort adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void seedIfEmpty() {
        if (adminUserRepository.findByUsername(DEFAULT_USERNAME).isPresent()) {
            return;
        }
        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);
        adminUserRepository.save(new AdminUser(DEFAULT_USERNAME, hash));
        log.info("[LocalSeed] 기본 관리자 계정 '{}' 을 생성했습니다.", DEFAULT_USERNAME);
    }
}
