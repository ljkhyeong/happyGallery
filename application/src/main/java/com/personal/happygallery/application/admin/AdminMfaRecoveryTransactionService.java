package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminMfaRecoveryTransactionService {

    private final AdminUserPort adminUserPort;
    private final PasswordEncoder passwordEncoder;
    private final AdminMfaResetService mfaResetService;

    AdminMfaRecoveryTransactionService(
            AdminUserPort adminUserPort,
            PasswordEncoder passwordEncoder,
            AdminMfaResetService mfaResetService
    ) {
        this.adminUserPort = adminUserPort;
        this.passwordEncoder = passwordEncoder;
        this.mfaResetService = mfaResetService;
    }

    @Transactional
    public void recover(Long adminUserId, String currentPassword) {
        AdminUser admin = adminUserPort.findByIdForUpdate(adminUserId)
                .orElseThrow(NotFoundException.supplier("관리자"));
        if (!admin.isMfaEnabled()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "MFA가 활성화되어 있지 않습니다.");
        }
        if (!passwordEncoder.matches(currentPassword, admin.getPasswordHash())) {
            throw invalidCredentials();
        }

        mfaResetService.resetAfterRecovery(admin);
    }

    private static HappyGalleryException invalidCredentials() {
        return new HappyGalleryException(
                ErrorCode.INVALID_CREDENTIALS, "관리자 인증 정보가 올바르지 않습니다.");
    }
}
