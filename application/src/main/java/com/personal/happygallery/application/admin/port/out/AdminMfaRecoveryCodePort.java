package com.personal.happygallery.application.admin.port.out;

import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import java.util.List;

public interface AdminMfaRecoveryCodePort {

    <S extends AdminMfaRecoveryCode> S save(S recoveryCode);

    <S extends AdminMfaRecoveryCode> List<S> saveAll(Iterable<S> recoveryCodes);

    List<AdminMfaRecoveryCode> findUnusedByAdminUserIdForUpdate(Long adminUserId);

    long countUnusedByAdminUserId(Long adminUserId);

    void deleteByAdminUserId(Long adminUserId);
}
