package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.in.AdminSetupUseCase;
import com.personal.happygallery.application.admin.port.out.AdminSetupLockPort;
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
    private final AdminSetupLockPort setupLock;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminSetupService(AdminUserPort adminUserRepository,
                                    AdminSetupLockPort setupLock,
                                    PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.setupLock = setupLock;
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
        String passwordHash = passwordEncoder.encode(rawPassword);
        setupLock.lock();
        if (adminUserRepository.count() != 0L) {
            throw new HappyGalleryException(ErrorCode.NOT_FOUND, "setup 이 이미 완료되었습니다.");
        }
        AdminUser saved = adminUserRepository.save(new AdminUser(username, passwordHash));
        log.warn("[AdminSetup] 최초 관리자 생성 [id={}] — 운영자는 즉시 로그인 후 비밀번호 rotate + ADMIN_SETUP_TOKEN env 제거",
                saved.getId());
    }
}
