package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAdminSetupService implements AdminSetupUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminSetupService.class);

    private final AdminUserPort adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminSetupService(AdminUserPort adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAvailable() {
        return adminUserRepository.count() == 0L;
    }

    @Override
    @Transactional
    public void setup(String username, String rawPassword) {
        if (adminUserRepository.count() != 0L) {
            throw new HappyGalleryException(ErrorCode.NOT_FOUND, "setup 이 이미 완료되었습니다.");
        }
        if (adminUserRepository.findByUsername(username).isPresent()) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS, "이미 사용 중인 username 입니다.");
        }
        AdminUser saved = adminUserRepository.save(new AdminUser(username, passwordEncoder.encode(rawPassword)));
        log.warn("[AdminSetup] 최초 관리자 '{}' (id={}) 생성됨 — 운영자는 즉시 로그인 후 비밀번호 rotate + ADMIN_SETUP_TOKEN env 제거",
                saved.getUsername(), saved.getId());
    }
}
