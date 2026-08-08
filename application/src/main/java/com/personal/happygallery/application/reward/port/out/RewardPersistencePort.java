package com.personal.happygallery.application.reward.port.out;

import com.personal.happygallery.domain.reward.RewardAccount;
import com.personal.happygallery.domain.reward.RewardLedger;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import com.personal.happygallery.domain.reward.RewardLot;
import com.personal.happygallery.domain.reward.RewardReservation;
import com.personal.happygallery.domain.reward.RewardReservationAllocation;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RewardPersistencePort {

    Optional<RewardAccount> findAccount(Long userId);

    Optional<RewardAccount> findAccountForUpdate(Long userId);

    RewardAccount saveAccount(RewardAccount account);

    List<RewardLot> findSpendableLotsForUpdate(Long userId, LocalDateTime now);

    List<RewardLot> findExpiredLotsForUpdate(Long userId, LocalDateTime now);

    List<RewardLot> findLotsByIdsForUpdate(Collection<Long> ids);

    List<RewardLot> findLotsBySourceOrderForUpdate(Long orderId);

    List<RewardLot> saveLots(Collection<RewardLot> lots);

    RewardReservation saveReservation(RewardReservation reservation);

    Optional<RewardReservation> findReservationByAttemptForUpdate(Long paymentAttemptId);

    Optional<RewardReservation> findReservationByOrderForUpdate(Long orderId);

    List<RewardReservationAllocation> findAllocations(Long reservationId);

    List<RewardReservationAllocation> saveAllocations(
            Collection<RewardReservationAllocation> allocations);

    RewardLedger saveLedger(RewardLedger ledger);

    boolean existsLedger(String idempotencyKey);

    long sumLedgerAmount(Long orderId, RewardLedgerType type);

    List<RewardLedger> findRecentLedger(Long userId, int limit);
}
