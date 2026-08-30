package com.personal.happygallery.adapter.out.persistence.reward;

import com.personal.happygallery.domain.reward.RewardLot;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardLotRepository extends JpaRepository<RewardLot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM RewardLot l
            WHERE l.userId = :userId
              AND l.remainingAmount > 0
              AND l.expiresAt > :now
            ORDER BY l.expiresAt ASC, l.id ASC
            """)
    List<RewardLot> findSpendableForUpdate(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM RewardLot l
            WHERE l.userId = :userId
              AND l.remainingAmount > 0
              AND l.expiresAt <= :now
            ORDER BY l.expiresAt ASC, l.id ASC
            """)
    List<RewardLot> findExpiredForUpdate(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM RewardLot l WHERE l.id IN :ids ORDER BY l.id ASC")
    List<RewardLot> findByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM RewardLot l
            WHERE l.sourceOrderId = :orderId
              AND l.remainingAmount > 0
            ORDER BY l.expiresAt ASC, l.id ASC
            """)
    List<RewardLot> findBySourceOrderIdForUpdate(@Param("orderId") Long orderId);
}
