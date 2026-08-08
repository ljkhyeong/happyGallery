package com.personal.happygallery.application.reward;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase;
import com.personal.happygallery.application.reward.port.out.RewardPersistencePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.reward.RewardAccount;
import com.personal.happygallery.domain.reward.RewardLedger;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import com.personal.happygallery.domain.reward.RewardLot;
import com.personal.happygallery.domain.reward.RewardReservation;
import com.personal.happygallery.domain.reward.RewardReservationAllocation;
import com.personal.happygallery.domain.reward.RewardReservationStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultRewardService implements RewardQueryUseCase, RewardBenefitService {

    static final int HISTORY_LIMIT = 100;
    static final int EXPIRY_YEARS = 1;
    static final int RESTORE_MINIMUM_DAYS = 30;

    private final RewardPersistencePort persistence;
    private final MemberAccountGuard memberAccountGuard;
    private final Clock clock;

    public DefaultRewardService(RewardPersistencePort persistence,
                                MemberAccountGuard memberAccountGuard,
                                Clock clock) {
        this.persistence = persistence;
        this.memberAccountGuard = memberAccountGuard;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RewardWallet getWallet(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        RewardAccount account = persistence.findAccountForUpdate(userId).orElse(null);
        if (account != null) {
            expireLots(account, now);
        }
        List<RewardHistory> history = persistence.findRecentLedger(userId, HISTORY_LIMIT).stream()
                .map(ledger -> new RewardHistory(
                        ledger.getId(), ledger.getType(), ledger.getAmount(),
                        ledger.getAvailableAfter(), ledger.getReservedAfter(), ledger.getDebtAfter(),
                        ledger.getOrderId(), ledger.getCreatedAt()))
                .toList();
        return account == null
                ? new RewardWallet(0L, 0L, 0L, history)
                : new RewardWallet(
                        account.getAvailableBalance(), account.getReservedBalance(),
                        account.getDebtBalance(), history);
    }

    @Override
    @Transactional
    public long quoteAndLock(
            Long userId, long requestedAmount, long maxUsableAmount, LocalDateTime now) {
        requireNonNegative(requestedAmount, "사용할 적립금");
        requireNonNegative(maxUsableAmount, "적립금 사용 상한");
        if (requestedAmount == 0L) {
            return 0L;
        }
        memberAccountGuard.requireActiveForUpdate(userId);
        RewardAccount account = persistence.findAccountForUpdate(userId)
                .orElseThrow(() -> new HappyGalleryException(
                        ErrorCode.REWARD_BALANCE_INSUFFICIENT, "사용 가능한 적립금이 없습니다."));
        expireLots(account, now);
        if (requestedAmount > maxUsableAmount || requestedAmount > account.getAvailableBalance()) {
            throw new HappyGalleryException(
                    ErrorCode.REWARD_BALANCE_INSUFFICIENT, "사용 가능한 적립금 범위를 초과했습니다.");
        }
        return requestedAmount;
    }

    @Override
    @Transactional
    public void reserve(Long userId, long amount, Long paymentAttemptId, LocalDateTime now) {
        if (amount == 0L) {
            return;
        }
        memberAccountGuard.requireActiveForUpdate(userId);
        if (persistence.findReservationByAttemptForUpdate(paymentAttemptId).isPresent()) {
            return;
        }
        RewardAccount account = accountForUpdate(userId);
        expireLots(account, now);
        account.reserve(amount);

        List<LotAllocation> selected = allocateLots(
                persistence.findSpendableLotsForUpdate(userId, now), amount, now);
        RewardReservation reservation = persistence.saveReservation(
                new RewardReservation(paymentAttemptId, userId, amount));
        List<RewardReservationAllocation> allocations = selected.stream()
                .map(allocation -> new RewardReservationAllocation(
                        reservation.getId(), allocation.lot().getId(), allocation.amount(),
                        allocation.lot().getExpiresAt()))
                .toList();
        persistence.saveLots(selected.stream().map(LotAllocation::lot).toList());
        persistence.saveAllocations(allocations);
        persistence.saveAccount(account);
        appendLedger(
                account, RewardLedgerType.RESERVE, amount, paymentAttemptId, null,
                "reward:reserve:attempt:" + paymentAttemptId);
    }

    @Override
    @Transactional
    public void release(Long paymentAttemptId, LocalDateTime now) {
        RewardReservation reservation = persistence.findReservationByAttemptForUpdate(paymentAttemptId)
                .orElse(null);
        if (reservation == null || reservation.getStatus() == RewardReservationStatus.RELEASED) {
            return;
        }
        if (reservation.getStatus() != RewardReservationStatus.RESERVED) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "사용 확정된 적립금은 예약 해제할 수 없습니다.");
        }
        memberAccountGuard.requireActiveForUpdate(reservation.getUserId());
        RewardAccount account = accountForUpdate(reservation.getUserId());
        expireLots(account, now);
        List<RewardReservationAllocation> allocations = persistence.findAllocations(reservation.getId());
        Map<Long, RewardLot> lots = lotsById(allocations);
        long returned = 0L;
        for (RewardReservationAllocation allocation : allocations) {
            if (!allocation.getOriginalExpiry().isAfter(now)) {
                continue;
            }
            account.release(allocation.getAmount());
            long availableCredit = account.credit(allocation.getAmount());
            if (availableCredit > 0L) {
                lots.get(allocation.getRewardLotId()).release(availableCredit);
            }
            returned = add(returned, allocation.getAmount());
        }
        if (returned > 0L) {
            appendLedger(
                    account, RewardLedgerType.RELEASE, returned, paymentAttemptId, null,
                    "reward:release:attempt:" + paymentAttemptId);
        }
        long expired = reservation.getAmount() - returned;
        if (expired > 0L) {
            account.expireReservation(expired);
            appendLedger(
                    account, RewardLedgerType.EXPIRE, expired, paymentAttemptId, null,
                    "reward:release-expire:attempt:" + paymentAttemptId);
        }
        reservation.release(now);
        persistence.saveLots(lots.values());
        persistence.saveAccount(account);
        persistence.saveReservation(reservation);
    }

    @Override
    @Transactional
    public void consume(
            Long paymentAttemptId, Long orderId, long expectedAmount, LocalDateTime now) {
        requireNonNegative(expectedAmount, "사용 확정 적립금");
        if (expectedAmount == 0L) {
            return;
        }
        RewardReservation reservation = persistence.findReservationByAttemptForUpdate(paymentAttemptId)
                .orElseThrow(() -> new HappyGalleryException(
                        ErrorCode.CONFLICT, "결제에 예약된 적립금 사용 이력이 없습니다."));
        if (reservation.getAmount() != expectedAmount) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT, "결제 적립금 예약액이 주문 확정 금액과 일치하지 않습니다.");
        }
        if (reservation.getStatus() == RewardReservationStatus.USED) {
            if (!orderId.equals(reservation.getOrderId())) {
                throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 사용 주문이 기존 기록과 다릅니다.");
            }
            return;
        }
        memberAccountGuard.requireActiveForUpdate(reservation.getUserId());
        RewardAccount account = accountForUpdate(reservation.getUserId());
        account.consume(reservation.getAmount());
        reservation.markUsed(orderId, now);
        persistence.saveAccount(account);
        persistence.saveReservation(reservation);
        appendLedger(
                account, RewardLedgerType.USE, reservation.getAmount(), paymentAttemptId, orderId,
                "reward:use:attempt:" + paymentAttemptId);
    }

    @Override
    @Transactional
    public void restoreUsed(
            Long orderId, long amount, String idempotencyKey, LocalDateTime now) {
        if (amount == 0L) {
            return;
        }
        requirePositive(amount, "복원 적립금");
        RewardReservation reservation = persistence.findReservationByOrderForUpdate(orderId)
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.CONFLICT, "복원할 적립금 사용 이력이 없습니다."));
        if (persistence.existsLedger(idempotencyKey)) {
            return;
        }
        if (amount > reservation.restorableAmount()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 복원액이 원래 사용액을 초과합니다.");
        }
        memberAccountGuard.requireActiveForUpdate(reservation.getUserId());
        RewardAccount account = accountForUpdate(reservation.getUserId());
        expireLots(account, now);
        List<RewardReservationAllocation> allocations = persistence.findAllocations(reservation.getId());
        List<RewardLot> restoredLots = new ArrayList<>();
        long remaining = amount;
        for (RewardReservationAllocation allocation : allocations) {
            long restored = allocation.restoreUpTo(remaining);
            if (restored == 0L) {
                continue;
            }
            long credited = account.credit(restored);
            if (credited > 0L) {
                LocalDateTime expiresAt = allocation.getOriginalExpiry().isAfter(now)
                        ? allocation.getOriginalExpiry()
                        : now.plusDays(RESTORE_MINIMUM_DAYS);
                restoredLots.add(new RewardLot(
                        reservation.getUserId(), orderId, credited, expiresAt));
            }
            remaining -= restored;
            if (remaining == 0L) {
                break;
            }
        }
        if (remaining != 0L) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 복원 배분이 원래 사용액과 일치하지 않습니다.");
        }
        reservation.recordRestored(amount);
        persistence.saveLots(restoredLots);
        persistence.saveAllocations(allocations);
        persistence.saveAccount(account);
        persistence.saveReservation(reservation);
        appendLedger(
                account, RewardLedgerType.RESTORE, amount, reservation.getPaymentAttemptId(),
                orderId, idempotencyKey);
    }

    @Override
    @Transactional
    public void accrue(Long userId, Long orderId, long amount, LocalDateTime earnedAt) {
        if (amount == 0L) {
            return;
        }
        requirePositive(amount, "적립 예정액");
        String idempotencyKey = "reward:earn:order:" + orderId;
        memberAccountGuard.requireActiveForUpdate(userId);
        if (persistence.existsLedger(idempotencyKey)) {
            return;
        }
        RewardAccount account = accountForUpdate(userId);
        expireLots(account, LocalDateTime.now(clock));
        long credited = account.credit(amount);
        if (credited > 0L) {
            persistence.saveLots(List.of(new RewardLot(
                    userId, orderId, credited, earnedAt.plusYears(EXPIRY_YEARS))));
        }
        persistence.saveAccount(account);
        appendLedger(
                account, RewardLedgerType.EARN, amount, null, orderId, idempotencyKey);
    }

    @Override
    @Transactional
    public void revokeEarned(
            Long userId, Long orderId, long amount, String idempotencyKey) {
        if (amount == 0L) {
            return;
        }
        requirePositive(amount, "회수 적립금");
        memberAccountGuard.requireActiveForUpdate(userId);
        if (persistence.existsLedger(idempotencyKey)) {
            return;
        }
        long earned = persistence.sumLedgerAmount(orderId, RewardLedgerType.EARN);
        long revoked = persistence.sumLedgerAmount(orderId, RewardLedgerType.REVOKE);
        if (amount > earned - revoked) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 회수액이 해당 주문 적립액을 초과합니다.");
        }
        RewardAccount account = accountForUpdate(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        expireLots(account, now);
        long availableDebit = account.revoke(amount);
        List<RewardLot> lots = new ArrayList<>(persistence.findLotsBySourceOrderForUpdate(orderId));
        long lotDebitRemaining = availableDebit;
        lotDebitRemaining = revokeFromLots(lots, lotDebitRemaining);
        if (lotDebitRemaining > 0L) {
            List<RewardLot> otherLots = persistence.findSpendableLotsForUpdate(
                    userId, now).stream()
                    .filter(lot -> !lots.contains(lot))
                    .toList();
            lotDebitRemaining = revokeFromLots(otherLots, lotDebitRemaining);
            lots.addAll(otherLots);
        }
        if (lotDebitRemaining != 0L) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 적립 단위 합계가 잔액과 일치하지 않습니다.");
        }
        persistence.saveLots(lots);
        persistence.saveAccount(account);
        appendLedger(
                account, RewardLedgerType.REVOKE, amount, null, orderId, idempotencyKey);
    }

    @Override
    @Transactional(readOnly = true)
    public RewardEarnedSnapshot getEarnedSnapshot(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId must not be null");
        }
        return new RewardEarnedSnapshot(
                persistence.sumLedgerAmount(orderId, RewardLedgerType.EARN),
                persistence.sumLedgerAmount(orderId, RewardLedgerType.REVOKE));
    }

    private RewardAccount accountForUpdate(Long userId) {
        return persistence.findAccountForUpdate(userId)
                .orElseGet(() -> persistence.saveAccount(RewardAccount.open(userId)));
    }

    private void expireLots(RewardAccount account, LocalDateTime now) {
        List<RewardLot> expiredLots = persistence.findExpiredLotsForUpdate(account.getUserId(), now);
        if (expiredLots.isEmpty()) {
            return;
        }
        for (RewardLot lot : expiredLots) {
            long expired = lot.expire(now);
            if (expired == 0L) {
                continue;
            }
            account.expire(expired);
            appendLedger(
                    account, RewardLedgerType.EXPIRE, expired, null, lot.getSourceOrderId(),
                    "reward:expire:lot:" + lot.getId());
        }
        persistence.saveLots(expiredLots);
        persistence.saveAccount(account);
    }

    private List<LotAllocation> allocateLots(
            List<RewardLot> lots, long requestedAmount, LocalDateTime now) {
        List<LotAllocation> allocations = new ArrayList<>();
        long remaining = requestedAmount;
        for (RewardLot lot : lots) {
            long allocated = lot.reserveUpTo(remaining, now);
            if (allocated > 0L) {
                allocations.add(new LotAllocation(lot, allocated));
                remaining -= allocated;
            }
            if (remaining == 0L) {
                break;
            }
        }
        if (remaining != 0L) {
            throw new HappyGalleryException(
                    ErrorCode.REWARD_BALANCE_INSUFFICIENT, "사용 가능한 적립금 적립 단위가 부족합니다.");
        }
        return allocations;
    }

    private Map<Long, RewardLot> lotsById(List<RewardReservationAllocation> allocations) {
        List<Long> lotIds = allocations.stream()
                .map(RewardReservationAllocation::getRewardLotId)
                .toList();
        Map<Long, RewardLot> lots = new HashMap<>();
        for (RewardLot lot : persistence.findLotsByIdsForUpdate(lotIds)) {
            lots.put(lot.getId(), lot);
        }
        if (lots.size() != lotIds.size()) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 예약의 적립 단위를 찾을 수 없습니다.");
        }
        return lots;
    }

    private long revokeFromLots(Collection<RewardLot> lots, long requested) {
        long remaining = requested;
        for (RewardLot lot : lots) {
            remaining -= lot.revokeUpTo(remaining);
            if (remaining == 0L) {
                break;
            }
        }
        return remaining;
    }

    private void appendLedger(RewardAccount account,
                              RewardLedgerType type,
                              long amount,
                              Long paymentAttemptId,
                              Long orderId,
                              String idempotencyKey) {
        if (persistence.existsLedger(idempotencyKey)) {
            return;
        }
        persistence.saveLedger(new RewardLedger(
                account.getUserId(), type, amount, account,
                paymentAttemptId, orderId, idempotencyKey));
    }

    private static long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 금액이 허용 범위를 초과했습니다.");
        }
    }

    private static void requirePositive(long amount, String field) {
        if (amount <= 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, field + "은 1원 이상이어야 합니다.");
        }
    }

    private static void requireNonNegative(long amount, String field) {
        if (amount < 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, field + "은 0원 이상이어야 합니다.");
        }
    }

    private record LotAllocation(RewardLot lot, long amount) {}
}
