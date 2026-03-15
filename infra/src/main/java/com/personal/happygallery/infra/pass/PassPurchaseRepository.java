package com.personal.happygallery.infra.pass;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PassPurchaseRepository extends JpaRepository<PassPurchase, Long> {

    /** 회원 — 자기 8회권 조회 (구매일 내림차순) */
    List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    /** guest claim preview용 비회원 8회권 조회 (구매일 내림차순) */
    @Query("""
            SELECT p FROM PassPurchase p
            JOIN FETCH p.guest
            WHERE p.guest.id = :guestId
            ORDER BY p.purchasedAt DESC
            """)
    List<PassPurchase> findByGuestIdOrderByPurchasedAtDesc(@Param("guestId") Long guestId);

    /** 만료 배치 대상: expires_at < now AND remaining_credits > 0 */
    List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(
            LocalDateTime now, int credits);

    /** 만료 7일 전 알림 대상: now <= expires_at < now+7일 AND remaining_credits > 0 */
    List<PassPurchase> findByExpiresAtBetweenAndRemainingCreditsGreaterThan(
            LocalDateTime start, LocalDateTime end, int credits);

    /** 만료 7일 전 알림 배치 — JOIN FETCH guest (detached 후 LAZY 로딩 방지) */
    @Query("SELECT p FROM PassPurchase p JOIN FETCH p.guest WHERE p.expiresAt >= :start AND p.expiresAt < :end AND p.remainingCredits > 0")
    List<PassPurchase> findExpiringWithGuestBetween(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);
}
