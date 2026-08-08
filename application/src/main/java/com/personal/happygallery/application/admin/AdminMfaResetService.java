package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.admin.AdminUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdminMfaResetService {

    private final AdminUserPort adminUserPort;
    private final AdminMfaRecoveryCodePort recoveryCodePort;
    private final AdminAuthAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    AdminMfaResetService(
            AdminUserPort adminUserPort,
            AdminMfaRecoveryCodePort recoveryCodePort,
            AdminAuthAuditService auditService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.adminUserPort = adminUserPort;
        this.recoveryCodePort = recoveryCodePort;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void resetAfterDisable(AdminUser admin) {
        reset(admin, AdminAuthOutcome.MFA_DISABLED);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void resetAfterRecovery(AdminUser admin) {
        reset(admin, AdminAuthOutcome.MFA_RECOVERY_RESET);
    }

    private void reset(AdminUser admin, AdminAuthOutcome outcome) {
        long invalidatedCredentialVersion = admin.disableMfa();
        recoveryCodePort.deleteByAdminUserId(admin.getId());
        adminUserPort.save(admin);
        auditService.record(admin.getId(), admin.getUsername(), outcome);
        eventPublisher.publishEvent(new AdminCredentialsChangedEvent(
                admin.getId(), invalidatedCredentialVersion));
    }
}
