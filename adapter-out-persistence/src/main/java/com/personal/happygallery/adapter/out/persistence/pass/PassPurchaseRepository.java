package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassPurchase;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassPurchaseRepository extends JpaRepository<PassPurchase, Long>, PassPurchaseReaderPort, PassPurchaseStorePort {

    @Override Optional<PassPurchase> findById(Long id);
    @Override PassPurchase save(PassPurchase passPurchase);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PassPurchase p WHERE p.id = :id")
    Optional<PassPurchase> findByIdForUpdate(@Param("id") Long id);

    /** 회원 — 자기 8회권 조회 (구매일 내림차순) */
    @Override List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
            FROM PassPurchase p
            WHERE p.userId = :userId
              AND p.remainingCredits > 0
              AND p.expiresAt > :now
            """)
    boolean existsUsableByUserId(@Param("userId") Long userId,
                                 @Param("now") LocalDateTime now);

    /** 만료 배치 페이지네이션 대상 */
    @Override
    @Query("""
            SELECT p FROM PassPurchase p
            WHERE p.expiresAt <= :now
              AND p.remainingCredits > :credits
              AND p.id > :afterId
            ORDER BY p.id ASC
            """)
    List<PassPurchase> findExpiredWithRemainingCreditsAfterId(
            @Param("now") LocalDateTime now,
            @Param("credits") int credits,
            @Param("afterId") Long afterId,
            Pageable pageable);

}
