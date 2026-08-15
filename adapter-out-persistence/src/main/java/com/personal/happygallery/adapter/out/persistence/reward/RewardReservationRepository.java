package com.personal.happygallery.adapter.out.persistence.reward;

import com.personal.happygallery.domain.reward.RewardReservation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardReservationRepository extends JpaRepository<RewardReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RewardReservation r WHERE r.paymentAttemptId = :paymentAttemptId")
    Optional<RewardReservation> findByPaymentAttemptIdForUpdate(
            @Param("paymentAttemptId") Long paymentAttemptId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RewardReservation r WHERE r.orderId = :orderId")
    Optional<RewardReservation> findByOrderIdForUpdate(@Param("orderId") Long orderId);
}
