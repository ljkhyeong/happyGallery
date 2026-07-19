package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAdminCredentialService implements AdminCredentialUseCase {

    private final AdminUserPort adminUserPort;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultAdminCredentialService(AdminUserPort adminUserPort,
                                         PasswordEncoder passwordEncoder,
                                         ApplicationEventPublisher eventPublisher) {
        this.adminUserPort = adminUserPort;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        AdminUser admin = adminUserPort.findByIdForUpdate(command.adminUserId())
                .orElseThrow(NotFoundException.supplier("관리자"));
        if (!passwordEncoder.matches(command.currentPassword(), admin.getPasswordHash())) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_CREDENTIALS, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (passwordEncoder.matches(command.newPassword(), admin.getPasswordHash())) {
            throw new HappyGalleryException(ErrorCode.PASSWORD_UNCHANGED);
        }

        long invalidatedCredentialVersion = admin.getCredentialVersion();
        admin.updatePasswordHash(passwordEncoder.encode(command.newPassword()));
        adminUserPort.save(admin);
        eventPublisher.publishEvent(new AdminCredentialsChangedEvent(
                admin.getId(), invalidatedCredentialVersion));
    }
}
