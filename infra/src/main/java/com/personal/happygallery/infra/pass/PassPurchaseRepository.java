package com.personal.happygallery.infra.pass;

import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassPurchaseRepository extends JpaRepository<PassPurchase, Long> {

    /** 만료 배치 대상: expires_at < now AND remaining_credits > 0 */
    List<PassPurchase> findByExpiresAtBeforeAndRemainingCreditsGreaterThan(
            LocalDateTime now, int credits);

    /** 만료 7일 전 알림 대상: now <= expires_at < now+7일 AND remaining_credits > 0 */
    List<PassPurchase> findByExpiresAtBetweenAndRemainingCreditsGreaterThan(
            LocalDateTime start, LocalDateTime end, int credits);
}
