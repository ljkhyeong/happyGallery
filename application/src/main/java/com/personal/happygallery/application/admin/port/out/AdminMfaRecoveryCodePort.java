package com.personal.happygallery.application.admin.port.out;

import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import java.util.List;

public interface AdminMfaRecoveryCodePort {

    AdminMfaRecoveryCode save(AdminMfaRecoveryCode recoveryCode);

    List<AdminMfaRecoveryCode> findUnusedByAdminUserIdForUpdate(Long adminUserId);

    long countUnusedByAdminUserId(Long adminUserId);

    void deleteByAdminUserId(Long adminUserId);
}
