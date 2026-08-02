package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminAuthHistoryPort;
import com.personal.happygallery.application.admin.port.out.AdminLoginSnapshot;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthHistory;
import com.personal.happygallery.domain.admin.AdminUser;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaAdminPersistenceAdapter implements AdminAuthHistoryPort, AdminUserPort {

    private final AdminAuthHistoryRepository authHistoryRepository;
    private final AdminUserRepository adminUserRepository;

    JpaAdminPersistenceAdapter(
            AdminAuthHistoryRepository authHistoryRepository,
            AdminUserRepository adminUserRepository) {
        this.authHistoryRepository = authHistoryRepository;
        this.adminUserRepository = adminUserRepository;
    }

    @Override
    public AdminAuthHistory save(AdminAuthHistory history) {
        return authHistoryRepository.save(history);
    }

    @Override
    public int deleteCreatedBefore(LocalDateTime cutoff, int limit) {
        return authHistoryRepository.deleteCreatedBefore(cutoff, limit);
    }

    @Override
    public Optional<AdminUser> findByUsername(String username) {
        return adminUserRepository.findByUsername(username);
    }

    @Override
    public Optional<AdminLoginSnapshot> findLoginSnapshotByUsername(String username) {
        return adminUserRepository.findLoginSnapshotByUsername(username);
    }

    @Override
    public Optional<AdminUser> findById(Long id) {
        return adminUserRepository.findById(id);
    }

    @Override
    public Optional<AdminUser> findByIdForUpdate(Long id) {
        return adminUserRepository.findByIdForUpdate(id);
    }

    @Override
    public AdminUser save(AdminUser adminUser) {
        return adminUserRepository.save(adminUser);
    }

    @Override
    public long count() {
        return adminUserRepository.count();
    }
}
