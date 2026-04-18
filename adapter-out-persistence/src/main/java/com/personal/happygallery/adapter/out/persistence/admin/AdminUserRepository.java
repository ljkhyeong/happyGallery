package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, AdminUserPort {

    @Override AdminUser save(AdminUser adminUser);

    Optional<AdminUser> findByUsername(String username);
}
