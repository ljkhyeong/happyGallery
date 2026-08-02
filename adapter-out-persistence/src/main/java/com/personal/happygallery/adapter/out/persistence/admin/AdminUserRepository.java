package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminLoginSnapshot;
import com.personal.happygallery.domain.admin.AdminUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);

    @Query("""
            SELECT new com.personal.happygallery.application.admin.port.out.AdminLoginSnapshot(
                a.id, a.username, a.passwordHash
            )
            FROM AdminUser a
            WHERE a.username = :username
            """)
    Optional<AdminLoginSnapshot> findLoginSnapshotByUsername(@Param("username") String username);

    @Override
    Optional<AdminUser> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminUser a WHERE a.id = :id")
    Optional<AdminUser> findByIdForUpdate(@Param("id") Long id);
}
