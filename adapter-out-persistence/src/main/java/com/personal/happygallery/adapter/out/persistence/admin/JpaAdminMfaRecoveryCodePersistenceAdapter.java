package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaAdminMfaRecoveryCodePersistenceAdapter implements AdminMfaRecoveryCodePort {

    private final AdminMfaRecoveryCodeRepository repository;

    JpaAdminMfaRecoveryCodePersistenceAdapter(AdminMfaRecoveryCodeRepository repository) {
        this.repository = repository;
    }

    @Override
    public AdminMfaRecoveryCode save(AdminMfaRecoveryCode recoveryCode) {
        return repository.save(recoveryCode);
    }

    @Override
    public List<AdminMfaRecoveryCode> findUnusedByAdminUserIdForUpdate(Long adminUserId) {
        return repository.findUnusedByAdminUserIdForUpdate(adminUserId);
    }

    @Override
    public long countUnusedByAdminUserId(Long adminUserId) {
        return repository.countUnusedByAdminUserId(adminUserId);
    }

    @Override
    public void deleteByAdminUserId(Long adminUserId) {
        repository.deleteByAdminUserId(adminUserId);
    }
}
