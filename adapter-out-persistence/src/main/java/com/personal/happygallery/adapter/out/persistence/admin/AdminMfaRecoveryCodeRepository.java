package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.domain.admin.AdminMfaRecoveryCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminMfaRecoveryCodeRepository extends JpaRepository<AdminMfaRecoveryCode, Long> {

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

    @Query("""
            SELECT COUNT(code)
              FROM AdminMfaRecoveryCode code
             WHERE code.adminUserId = :adminUserId
               AND code.usedAt IS NULL
            """)
    long countUnusedByAdminUserId(@Param("adminUserId") Long adminUserId);

    void deleteByAdminUserId(Long adminUserId);
}
