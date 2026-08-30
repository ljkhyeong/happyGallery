package com.personal.happygallery.adapter.out.persistence.reward;

import com.personal.happygallery.application.reward.port.out.RewardPersistencePort;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class JpaRewardPersistenceAdapter implements RewardPersistencePort {

    private final RewardAccountRepository accountRepository;
    private final RewardLotRepository lotRepository;
    private final RewardReservationRepository reservationRepository;
    private final RewardReservationAllocationRepository allocationRepository;
    private final RewardLedgerRepository ledgerRepository;

    JpaRewardPersistenceAdapter(RewardAccountRepository accountRepository,
                                RewardLotRepository lotRepository,
                                RewardReservationRepository reservationRepository,
                                RewardReservationAllocationRepository allocationRepository,
                                RewardLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.lotRepository = lotRepository;
        this.reservationRepository = reservationRepository;
        this.allocationRepository = allocationRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public Optional<RewardAccount> findAccount(Long userId) {
        return accountRepository.findById(userId);
    }

    @Override
    public Optional<RewardAccount> findAccountForUpdate(Long userId) {
        return accountRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public RewardAccount saveAccount(RewardAccount account) {
        return accountRepository.save(account);
    }

    @Override
    public List<RewardLot> findSpendableLotsForUpdate(Long userId, LocalDateTime now) {
        return lotRepository.findSpendableForUpdate(userId, now);
    }

    @Override
    public List<RewardLot> findExpiredLotsForUpdate(Long userId, LocalDateTime now) {
        return lotRepository.findExpiredForUpdate(userId, now);
    }

    @Override
    public List<RewardLot> findLotsByIdsForUpdate(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : lotRepository.findByIdInForUpdate(ids);
    }

    @Override
    public List<RewardLot> findLotsBySourceOrderForUpdate(Long orderId) {
        return lotRepository.findBySourceOrderIdForUpdate(orderId);
    }

    @Override
    public List<RewardLot> saveLots(Collection<RewardLot> lots) {
        return lotRepository.saveAll(lots);
    }

    @Override
    public RewardReservation saveReservation(RewardReservation reservation) {
        return reservationRepository.save(reservation);
    }

    @Override
    public Optional<RewardReservation> findReservationByAttemptForUpdate(Long paymentAttemptId) {
        return reservationRepository.findByPaymentAttemptIdForUpdate(paymentAttemptId);
    }

    @Override
    public Optional<RewardReservation> findReservationByOrderForUpdate(Long orderId) {
        return reservationRepository.findByOrderIdForUpdate(orderId);
    }

    @Override
    public List<RewardReservationAllocation> findAllocations(Long reservationId) {
        return allocationRepository.findByReservationIdOrderByIdAsc(reservationId);
    }

    @Override
    public List<RewardReservationAllocation> saveAllocations(
            Collection<RewardReservationAllocation> allocations) {
        return allocationRepository.saveAll(allocations);
    }

    @Override
    public RewardLedger saveLedger(RewardLedger ledger) {
        return ledgerRepository.save(ledger);
    }

    @Override
    public boolean existsLedger(String idempotencyKey) {
        return ledgerRepository.existsByIdempotencyKey(idempotencyKey);
    }

    @Override
    public long sumLedgerAmount(Long orderId, RewardLedgerType type) {
        return ledgerRepository.sumAmountByOrderIdAndType(orderId, type);
    }

    @Override
    public List<RewardLedger> findRecentLedger(Long userId, int limit) {
        return ledgerRepository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.ofSize(limit));
    }
}
