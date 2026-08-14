package com.personal.happygallery.adapter.out.persistence.coupon;

import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponDefinitionRepository
        extends JpaRepository<CouponDefinition, Long>,
        CouponDefinitionReaderPort,
        CouponDefinitionStorePort {

    @Override
    <S extends CouponDefinition> S saveAndFlush(S definition);

    @Override
    Optional<CouponDefinition> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponDefinition c WHERE c.id = :id")
    Optional<CouponDefinition> findByIdForUpdate(@Param("id") Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT c FROM CouponDefinition c WHERE c.id = :id")
    Optional<CouponDefinition> findByIdForClaim(@Param("id") Long id);

    @Override
    List<CouponDefinition> findAllById(Iterable<Long> ids);

    @Override
    List<CouponDefinition> findAllByOrderByIdDesc();

    @Query("""
            SELECT definition
            FROM CouponDefinition definition
            WHERE definition.active = true
              AND definition.publiclyClaimable = true
              AND definition.validFrom <= :now
              AND definition.validUntil > :now
              AND NOT EXISTS (
                  SELECT issued.id
                  FROM IssuedCoupon issued
                  WHERE issued.userId = :userId
                    AND issued.definitionId = definition.id
              )
            ORDER BY definition.id DESC
            """)
    List<CouponDefinition> findClaimableByUserIdPage(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Override
    default List<CouponDefinition> findClaimableByUserId(
            Long userId, LocalDateTime now, int limit) {
        return findClaimableByUserIdPage(userId, now, PageRequest.ofSize(limit));
    }
}
