package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminMfaRecoveryCodeRepository
        extends JpaRepository<AdminMfaRecoveryCode, Long>, AdminMfaRecoveryCodePort {

    @Override
    AdminMfaRecoveryCode save(AdminMfaRecoveryCode recoveryCode);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT code
              FROM AdminMfaRecoveryCode code
             WHERE code.adminUserId = :adminUserId
               AND code.usedAt IS NULL
             ORDER BY code.id
            """)
    List<AdminMfaRecoveryCode> findUnusedByAdminUserIdForUpdate(
            @Param("adminUserId") Long adminUserId);

    @Override
    @Query("""
            SELECT COUNT(code)
              FROM AdminMfaRecoveryCode code
             WHERE code.adminUserId = :adminUserId
               AND code.usedAt IS NULL
            """)
    long countUnusedByAdminUserId(@Param("adminUserId") Long adminUserId);

    @Override
    void deleteByAdminUserId(Long adminUserId);
}
