package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.RestockAlertPort;
import com.personal.happygallery.domain.product.RestockAlert;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestockAlertRepository extends JpaRepository<RestockAlert, Long>, RestockAlertPort {
    @Override <S extends RestockAlert> S saveAndFlush(S alert);
    @Override Optional<RestockAlert> findByActiveKey(String key);
    @Override List<RestockAlert> findByUserIdOrderByIdDesc(Long userId);
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM RestockAlert a WHERE a.id = :id")
    Optional<RestockAlert> findByIdForUpdate(@Param("id") Long id);
}
