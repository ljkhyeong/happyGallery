package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, AdminUserPort {

    @Override AdminUser save(AdminUser adminUser);

    @Override Optional<AdminUser> findByUsername(String username);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminUser a WHERE a.username = :username")
    Optional<AdminUser> findByUsernameForUpdate(@Param("username") String username);

    @Override Optional<AdminUser> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminUser a WHERE a.id = :id")
    Optional<AdminUser> findByIdForUpdate(@Param("id") Long id);
}
