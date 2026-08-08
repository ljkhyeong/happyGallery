package com.personal.happygallery.adapter.out.persistence.coupon;

import com.personal.happygallery.application.coupon.port.out.IssuedCouponReaderPort;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssuedCouponRepository
        extends JpaRepository<IssuedCoupon, Long>, IssuedCouponReaderPort {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM IssuedCoupon c WHERE c.id = :id")
    Optional<IssuedCoupon> findByIdForUpdate(@Param("id") Long id);

    @Override
    Optional<IssuedCoupon> findByUserIdAndDefinitionId(Long userId, Long definitionId);

    @Override
    boolean existsByDefinitionId(Long definitionId);

    @Override
    List<IssuedCoupon> findByUserIdOrderByClaimedAtDescIdDesc(Long userId);

    @Override
    List<IssuedCoupon> findTop100ByUserIdOrderByClaimedAtDescIdDesc(Long userId);
}
