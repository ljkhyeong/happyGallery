package com.personal.happygallery.domain.reward;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "reward_accounts")
public class RewardAccount {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "available_balance", nullable = false)
    private long availableBalance;

    @Column(name = "reserved_balance", nullable = false)
    private long reservedBalance;

    @Column(name = "debt_balance", nullable = false)
    private long debtBalance;

    @Version
    @Column(nullable = false)
    private long version;

    protected RewardAccount() {}

    private RewardAccount(Long userId) {
        if (userId == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 회원은 필수입니다.");
        }
        this.userId = userId;
    }

    public static RewardAccount open(Long userId) {
        return new RewardAccount(userId);
    }

    public void reserve(long amount) {
        requirePositive(amount);
        if (availableBalance < amount) {
            throw new HappyGalleryException(
                    ErrorCode.REWARD_BALANCE_INSUFFICIENT, "사용 가능한 적립금이 부족합니다.");
        }
        availableBalance = subtract(availableBalance, amount);
        reservedBalance = add(reservedBalance, amount);
    }

    /** 만료되지 않은 예약액을 예약 잔액에서 해제한다. 실제 반환은 적립 단위별로 {@link #credit(long)}한다. */
    public void release(long amount) {
        closeReservation(amount);
    }

    /** 예약 중 만료된 적립액을 사용 가능 잔액으로 돌리지 않고 예약 잔액에서 제거한다. */
    public void expireReservation(long amount) {
        closeReservation(amount);
    }

    public void consume(long amount) {
        requirePositive(amount);
        if (reservedBalance < amount) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "예약된 적립금 잔액이 일치하지 않습니다.");
        }
        reservedBalance = subtract(reservedBalance, amount);
    }

    /** 적립 또는 복원 금액을 먼저 미회수 부채에 충당하고 실제 사용 가능 증가액을 반환한다. */
    public long credit(long amount) {
        requirePositive(amount);
        long debtRepaid = Math.min(debtBalance, amount);
        debtBalance = subtract(debtBalance, debtRepaid);
        long credited = subtract(amount, debtRepaid);
        availableBalance = add(availableBalance, credited);
        return credited;
    }

    /** 회수할 적립금이 이미 사용됐다면 부족분을 부채로 남긴다. */
    public long revoke(long amount) {
        requirePositive(amount);
        long availableDebit = Math.min(availableBalance, amount);
        availableBalance = subtract(availableBalance, availableDebit);
        debtBalance = add(debtBalance, subtract(amount, availableDebit));
        return availableDebit;
    }

    public void expire(long amount) {
        requirePositive(amount);
        if (availableBalance < amount) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 만료 잔액이 원장과 일치하지 않습니다.");
        }
        availableBalance = subtract(availableBalance, amount);
    }

    private void closeReservation(long amount) {
        requirePositive(amount);
        if (reservedBalance < amount) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "예약된 적립금 잔액이 일치하지 않습니다.");
        }
        reservedBalance = subtract(reservedBalance, amount);
    }

    private static void requirePositive(long amount) {
        if (amount <= 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 금액은 1원 이상이어야 합니다.");
        }
    }

    private static long add(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "적립금 금액이 허용 범위를 초과했습니다.");
        }
    }

    private static long subtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException exception) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "적립금 잔액 계산이 올바르지 않습니다.");
        }
    }

    public Long getUserId() { return userId; }
    public long getAvailableBalance() { return availableBalance; }
    public long getReservedBalance() { return reservedBalance; }
    public long getDebtBalance() { return debtBalance; }
    public long getVersion() { return version; }
}
