package com.personal.happygallery.adapter.out.persistence.reward;

import com.personal.happygallery.domain.reward.RewardLedger;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardLedgerRepository extends JpaRepository<RewardLedger, Long> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(l.amount), 0)
            FROM RewardLedger l
            WHERE l.orderId = :orderId
              AND l.type = :type
            """)
    long sumAmountByOrderIdAndType(
            @Param("orderId") Long orderId,
            @Param("type") RewardLedgerType type);

    List<RewardLedger> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);
}
