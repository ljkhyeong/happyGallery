package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassPurchase;
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

public interface PassPurchaseRepository extends JpaRepository<PassPurchase, Long>,
        PassPurchaseReaderPort,
        PassPurchaseStorePort {

    @Override
    <S extends PassPurchase> S save(S passPurchase);

    @Override Optional<PassPurchase> findById(Long id);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PassPurchase p WHERE p.id = :id")
    Optional<PassPurchase> findByIdForUpdate(@Param("id") Long id);

    /** 회원 — 자기 8회권 조회 (구매일 내림차순) */
    List<PassPurchase> findByUserIdOrderByPurchasedAtDescIdDesc(
            Long userId, Pageable pageable);

    @Query("""
            SELECT p FROM PassPurchase p
            WHERE p.userId = :userId
              AND (p.purchasedAt < :purchasedAt
                   OR (p.purchasedAt = :purchasedAt AND p.id < :id))
            ORDER BY p.purchasedAt DESC, p.id DESC
            """)
    List<PassPurchase> findByUserIdOrderByPurchasedAtDescAfterPage(
            @Param("userId") Long userId,
            @Param("purchasedAt") LocalDateTime purchasedAt,
            @Param("id") Long id,
            Pageable pageable);

    @Override
    default List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId, int limit) {
        return findByUserIdOrderByPurchasedAtDescIdDesc(
                userId, PageRequest.ofSize(limit));
    }

    @Override
    default List<PassPurchase> findByUserIdOrderByPurchasedAtDescAfter(
            Long userId, LocalDateTime purchasedAt, Long id, int limit) {
        return findByUserIdOrderByPurchasedAtDescAfterPage(
                userId, purchasedAt, id, PageRequest.ofSize(limit));
    }

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
    @Query("""
            SELECT p FROM PassPurchase p
            WHERE p.expiresAt <= :now
              AND p.remainingCredits > :credits
              AND p.id > :afterId
            ORDER BY p.id ASC
            """)
    List<PassPurchase> findExpiredWithRemainingCreditsAfterIdPage(
            @Param("now") LocalDateTime now,
            @Param("credits") int credits,
            @Param("afterId") Long afterId,
            Pageable pageable);

    @Override
    default List<PassPurchase> findExpiredWithRemainingCreditsAfterId(
            LocalDateTime now, int credits, Long afterId, int limit) {
        return findExpiredWithRemainingCreditsAfterIdPage(
                now, credits, afterId, PageRequest.ofSize(limit));
    }
}
