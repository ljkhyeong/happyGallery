package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminAuthHistoryPort;
import com.personal.happygallery.domain.admin.AdminAuthHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminAuthHistoryRepository extends JpaRepository<AdminAuthHistory, Long>,
        AdminAuthHistoryPort {

    @Override
    <S extends AdminAuthHistory> S save(S history);

    @Override
    @Modifying
    @Query(value = """
            DELETE FROM admin_auth_history
             WHERE created_at < :cutoff
             ORDER BY id
             LIMIT :limit
            """, nativeQuery = true)
    int deleteCreatedBefore(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit);

    List<AdminAuthHistory> findAllByOrderByIdAsc();
}
