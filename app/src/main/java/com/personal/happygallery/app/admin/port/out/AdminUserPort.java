package com.personal.happygallery.app.admin.port.out;

import com.personal.happygallery.domain.admin.AdminUser;
import java.util.Optional;

/**
 * 관리자 계정 조회/저장 포트.
 */
public interface AdminUserPort {

    Optional<AdminUser> findByUsername(String username);

    AdminUser save(AdminUser adminUser);
}
