package com.personal.happygallery.adapter.out.persistence.coupon;

import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponDefinitionRepository
        extends JpaRepository<CouponDefinition, Long>, CouponDefinitionReaderPort {

    @Override
    Optional<CouponDefinition> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponDefinition c WHERE c.id = :id")
    Optional<CouponDefinition> findByIdForUpdate(@Param("id") Long id);

    @Override
    List<CouponDefinition> findAllById(Iterable<Long> ids);

    @Override
    List<CouponDefinition> findAllByOrderByIdDesc();
}
