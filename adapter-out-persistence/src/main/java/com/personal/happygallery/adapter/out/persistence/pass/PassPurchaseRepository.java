package com.personal.happygallery.adapter.out.persistence.pass;

import com.personal.happygallery.application.pass.port.out.PassPurchaseReaderPort;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassPurchaseRepository extends JpaRepository<PassPurchase, Long>, PassPurchaseReaderPort, PassPurchaseStorePort {

    @Override Optional<PassPurchase> findById(Long id);
    @Override PassPurchase save(PassPurchase passPurchase);

    /** 회원 — 자기 8회권 조회 (구매일 내림차순) */
    List<PassPurchase> findByUserIdOrderByPurchasedAtDesc(Long userId);

    /** 만료 배치 대상: expires_at < now AND remaining_credits > 0 */
    List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(
            LocalDateTime now, int credits);

    /** 만료 배치 페이지네이션 대상 */
    List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(
            LocalDateTime now, int credits, Pageable pageable);

    /** 만료 7일 전 알림 대상: now <= expires_at < now+7일 AND remaining_credits > 0 */
    List<PassPurchase> findByExpiresAtBetweenAndRemainingCreditsGreaterThan(
            LocalDateTime start, LocalDateTime end, int credits);
}
