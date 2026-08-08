package com.personal.happygallery.application.reward;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@UseCaseIT
class RewardUseCaseIT {

    @Autowired RewardBenefitService rewardBenefitService;
    @Autowired RewardQueryUseCase rewardQueryUseCase;
    @Autowired UserStorePort userStorePort;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("적립금은 결제 시도에 예약한 뒤 해제하거나 주문 사용으로 확정하고 멱등 복원한다")
    void reserveReleaseConsumeAndRestore() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-flow@example.com", "01024001000");
        Order earningOrder = saveOrder(user.getId(), 100_000L, now);
        rewardBenefitService.accrue(user.getId(), earningOrder.getId(), 1_000L, now);

        PaymentAttempt releasedAttempt = saveAttempt(user.getId(), "reward-release", 99_400L);
        assertThat(rewardBenefitService.quoteAndLock(
                user.getId(), 600L, 100_000L, now)).isEqualTo(600L);
        rewardBenefitService.reserve(user.getId(), 600L, releasedAttempt.getId(), now);
        rewardBenefitService.release(releasedAttempt.getId(), now.plusMinutes(1));

        PaymentAttempt usedAttempt = saveAttempt(user.getId(), "reward-use", 99_500L);
        rewardBenefitService.reserve(user.getId(), 500L, usedAttempt.getId(), now.plusMinutes(2));
        Order spendingOrder = saveOrder(user.getId(), 100_000L, now.plusMinutes(2));
        rewardBenefitService.consume(
                usedAttempt.getId(), spendingOrder.getId(), 500L, now.plusMinutes(3));
        rewardBenefitService.restoreUsed(
                spendingOrder.getId(), 300L, "test:reward:restore", now.plusMinutes(4));
        rewardBenefitService.restoreUsed(
                spendingOrder.getId(), 300L, "test:reward:restore", now.plusMinutes(5));

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertSoftly(softly -> {
            softly.assertThat(wallet.availableBalance()).isEqualTo(800L);
            softly.assertThat(wallet.reservedBalance()).isZero();
            softly.assertThat(wallet.debtBalance()).isZero();
            softly.assertThat(wallet.history())
                    .extracting(RewardQueryUseCase.RewardHistory::type)
                    .contains(RewardLedgerType.EARN, RewardLedgerType.RESERVE,
                            RewardLedgerType.RELEASE, RewardLedgerType.USE,
                            RewardLedgerType.RESTORE);
        });
    }

    @Test
    @DisplayName("적립금 사용 확정은 주문 스냅샷과 같은 예약만 허용하고 0원은 예약 없이 통과한다")
    void consume_requiresReservationMatchingExpectedAmount() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-consume-boundary@example.com", "01024001500");
        Order order = saveOrder(user.getId(), 100_000L, now);
        PaymentAttempt missingAttempt = saveAttempt(user.getId(), "reward-missing", 100_000L);

        assertThatCode(() -> rewardBenefitService.consume(
                missingAttempt.getId(), order.getId(), 0L, now))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> rewardBenefitService.consume(
                missingAttempt.getId(), order.getId(), 100L, now))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));

        Order earningOrder = saveOrder(user.getId(), 100_000L, now);
        rewardBenefitService.accrue(user.getId(), earningOrder.getId(), 100L, now);
        PaymentAttempt mismatchedAttempt = saveAttempt(user.getId(), "reward-mismatch", 99_900L);
        rewardBenefitService.reserve(user.getId(), 100L, mismatchedAttempt.getId(), now);

        assertThatThrownBy(() -> rewardBenefitService.consume(
                mismatchedAttempt.getId(), order.getId(), 99L, now))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessageContaining("예약액");
                });
    }

    @Test
    @DisplayName("만료되거나 이미 사용한 적립금 회수 부족분은 부채가 되고 이후 적립금으로 먼저 상환한다")
    void expiryAndDebtRepayment() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-debt@example.com", "01024002000");
        Order expiredOrder = saveOrder(user.getId(), 100_000L, now.minusYears(2));
        rewardBenefitService.accrue(
                user.getId(), expiredOrder.getId(), 1_000L, now.minusYears(2));

        assertThat(rewardQueryUseCase.getWallet(user.getId()).availableBalance()).isZero();

        rewardBenefitService.revokeEarned(
                user.getId(), expiredOrder.getId(), 1_000L, "test:reward:revoke");
        Order laterOrder = saveOrder(user.getId(), 150_000L, now);
        rewardBenefitService.accrue(user.getId(), laterOrder.getId(), 1_500L, now);

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertSoftly(softly -> {
            softly.assertThat(wallet.availableBalance()).isEqualTo(500L);
            softly.assertThat(wallet.debtBalance()).isZero();
            softly.assertThat(wallet.history())
                    .extracting(RewardQueryUseCase.RewardHistory::type)
                    .contains(RewardLedgerType.EXPIRE, RewardLedgerType.REVOKE);
        });
    }

    @Test
    @DisplayName("지갑 조회 전 만료된 적립 단위만 남아 있어도 회수는 만료를 먼저 반영하고 부채로 기록한다")
    void revokeEarned_expiresLazyLotsBeforeDebtCalculation() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-lazy-expiry@example.com", "01024002500");
        Order expiredOrder = saveOrder(user.getId(), 100_000L, now.minusYears(2));
        rewardBenefitService.accrue(
                user.getId(), expiredOrder.getId(), 1_000L, now.minusYears(2));

        rewardBenefitService.revokeEarned(
                user.getId(), expiredOrder.getId(), 1_000L, "test:reward:lazy-expiry-revoke");

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertSoftly(softly -> {
            softly.assertThat(wallet.availableBalance()).isZero();
            softly.assertThat(wallet.debtBalance()).isEqualTo(1_000L);
            softly.assertThat(wallet.history())
                    .extracting(RewardQueryUseCase.RewardHistory::type)
                    .containsSubsequence(RewardLedgerType.REVOKE, RewardLedgerType.EXPIRE);
        });
    }

    @Test
    @DisplayName("예약 중인 적립금을 회수한 뒤 예약이 해제되면 반환액은 부채를 먼저 상환한다")
    void releaseAfterRevoke_repaymentPrecedesReusableBalance() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-reserved-debt@example.com", "01024002700");
        Order earningOrder = saveOrder(user.getId(), 100_000L, now);
        rewardBenefitService.accrue(user.getId(), earningOrder.getId(), 100L, now);
        PaymentAttempt attempt = saveAttempt(user.getId(), "reward-reserved-debt", 99_900L);
        rewardBenefitService.reserve(user.getId(), 100L, attempt.getId(), now.plusMinutes(1));

        rewardBenefitService.revokeEarned(
                user.getId(), earningOrder.getId(), 100L, "test:reward:reserved-revoke");
        rewardBenefitService.release(attempt.getId(), now.plusMinutes(2));

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertSoftly(softly -> {
            softly.assertThat(wallet.availableBalance()).isZero();
            softly.assertThat(wallet.reservedBalance()).isZero();
            softly.assertThat(wallet.debtBalance()).isZero();
        });
        assertThatThrownBy(() -> rewardBenefitService.quoteAndLock(
                user.getId(), 1L, 1L, now.plusMinutes(3)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.REWARD_BALANCE_INSUFFICIENT));
    }

    @Test
    @DisplayName("여러 만료 단위의 예약 반환은 부채를 앞 단위부터 상환해 뒤 만료 잔액을 보존한다")
    void releaseAfterMultiLotRevoke_preservesLaterExpiry() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-multi-lot-debt@example.com", "01024002800");
        Order shortLivedOrder = saveOrder(user.getId(), 30_000L, now.minusYears(1).plusDays(1));
        Order longLivedOrder = saveOrder(user.getId(), 70_000L, now);
        rewardBenefitService.accrue(
                user.getId(), shortLivedOrder.getId(), 30L, now.minusYears(1).plusDays(1));
        rewardBenefitService.accrue(user.getId(), longLivedOrder.getId(), 70L, now);
        PaymentAttempt attempt = saveAttempt(user.getId(), "reward-multi-lot-debt", 99_900L);
        rewardBenefitService.reserve(user.getId(), 100L, attempt.getId(), now.plusMinutes(1));

        rewardBenefitService.revokeEarned(
                user.getId(), shortLivedOrder.getId(), 30L, "test:reward:short-lot-revoke");
        rewardBenefitService.revokeEarned(
                user.getId(), longLivedOrder.getId(), 20L, "test:reward:long-lot-revoke");
        rewardBenefitService.release(attempt.getId(), now.plusMinutes(2));

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertSoftly(softly -> {
            softly.assertThat(wallet.availableBalance()).isEqualTo(50L);
            softly.assertThat(wallet.reservedBalance()).isZero();
            softly.assertThat(wallet.debtBalance()).isZero();
        });
        assertThatCode(() -> rewardBenefitService.quoteAndLock(
                user.getId(), 50L, 50L, now.plusDays(2)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("예약 해제 중 일부가 만료되면 해제와 만료 원장은 서로 다른 잔액 시점을 기록한다")
    void releaseWithPartialExpiry_recordsReproducibleLedgerSnapshots() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-release-expiry-ledger@example.com", "01024002850");
        Order shortLivedOrder = saveOrder(user.getId(), 30_000L, now.minusYears(1).plusMinutes(1));
        Order longLivedOrder = saveOrder(user.getId(), 70_000L, now);
        rewardBenefitService.accrue(
                user.getId(), shortLivedOrder.getId(), 30L, now.minusYears(1).plusMinutes(1));
        rewardBenefitService.accrue(user.getId(), longLivedOrder.getId(), 70L, now);
        PaymentAttempt attempt = saveAttempt(user.getId(), "reward-partial-expiry", 99_900L);
        rewardBenefitService.reserve(user.getId(), 100L, attempt.getId(), now);

        rewardBenefitService.release(attempt.getId(), now.plusMinutes(2));

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        RewardQueryUseCase.RewardHistory released = wallet.history().stream()
                .filter(history -> history.type() == RewardLedgerType.RELEASE)
                .findFirst()
                .orElseThrow();
        RewardQueryUseCase.RewardHistory expired = wallet.history().stream()
                .filter(history -> history.type() == RewardLedgerType.EXPIRE
                        && history.amount() == 30L)
                .findFirst()
                .orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(released.amount()).isEqualTo(70L);
            softly.assertThat(released.reservedAfter()).isEqualTo(30L);
            softly.assertThat(expired.amount()).isEqualTo(30L);
            softly.assertThat(expired.reservedAfter()).isZero();
            softly.assertThat(wallet.availableBalance()).isEqualTo(70L);
        });
    }

    @Test
    @DisplayName("같은 멱등 키의 적립금 복원 동시 요청은 한 번만 반영하고 모두 성공한다")
    void restoreUsed_sameIdempotencyKeyConcurrentlyAppliesOnce() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-restore-idempotency@example.com", "01024002900");
        Order earningOrder = saveOrder(user.getId(), 60_000L, now);
        rewardBenefitService.accrue(user.getId(), earningOrder.getId(), 60L, now);
        PaymentAttempt attempt = saveAttempt(user.getId(), "reward-restore-idempotency", 59_940L);
        rewardBenefitService.reserve(user.getId(), 60L, attempt.getId(), now);
        Order spendingOrder = saveOrder(user.getId(), 60_000L, now);
        rewardBenefitService.consume(attempt.getId(), spendingOrder.getId(), 60L, now);

        runConcurrently(5, () -> rewardBenefitService.restoreUsed(
                spendingOrder.getId(), 30L, "test:reward:concurrent-restore", now.plusMinutes(1)));

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertThat(wallet.availableBalance()).isEqualTo(30L);
        assertThat(wallet.history())
                .filteredOn(history -> history.type() == RewardLedgerType.RESTORE)
                .hasSize(1);
    }

    @Test
    @DisplayName("같은 멱등 키의 적립금 회수 동시 요청은 한 번만 반영하고 모두 성공한다")
    void revokeEarned_sameIdempotencyKeyConcurrentlyAppliesOnce() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-revoke-idempotency@example.com", "01024002950");
        Order earningOrder = saveOrder(user.getId(), 100_000L, now);
        rewardBenefitService.accrue(user.getId(), earningOrder.getId(), 100L, now);

        runConcurrently(5, () -> rewardBenefitService.revokeEarned(
                user.getId(), earningOrder.getId(), 40L, "test:reward:concurrent-revoke"));

        RewardQueryUseCase.RewardWallet wallet = rewardQueryUseCase.getWallet(user.getId());
        assertThat(wallet.availableBalance()).isEqualTo(60L);
        assertThat(wallet.history())
                .filteredOn(history -> history.type() == RewardLedgerType.REVOKE)
                .hasSize(1);
    }

    @Test
    @DisplayName("적립금 원장 조회는 생성 시각과 식별자 역순의 최근 100건으로 제한한다")
    void getWallet_limitsStableRecentHistory() {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = createUser("reward-history@example.com", "01024003000");
        List<Order> orders = IntStream.rangeClosed(1, 101)
                .mapToObj(index -> saveOrder(user.getId(), 10_000L, now))
                .toList();
        orders.forEach(order -> rewardBenefitService.accrue(
                user.getId(), order.getId(), 1L, now));

        var history = rewardQueryUseCase.getWallet(user.getId()).history();
        List<Long> expectedOrderIds = orders.stream()
                .map(Order::getId)
                .sorted(Comparator.reverseOrder())
                .limit(100)
                .toList();

        assertSoftly(softly -> {
            softly.assertThat(history).hasSize(100);
            softly.assertThat(history)
                    .extracting(RewardQueryUseCase.RewardHistory::orderId)
                    .containsExactlyElementsOf(expectedOrderIds);
            softly.assertThat(history)
                    .extracting(RewardQueryUseCase.RewardHistory::id)
                    .isSortedAccordingTo(Comparator.reverseOrder());
        });
    }

    private User createUser(String email, String phone) {
        return userStorePort.save(new User(email, "password-hash", "적립금 회원", phone));
    }

    private Order saveOrder(Long userId, long amount, LocalDateTime paidAt) {
        return orderRepository.saveAndFlush(Order.forMember(
                userId, amount, paidAt, paidAt.plusHours(24)));
    }

    private PaymentAttempt saveAttempt(Long userId, String externalOrderId, long amount) {
        return paymentAttemptRepository.saveAndFlush(PaymentAttempt.startForMember(
                externalOrderId, PaymentContext.ORDER, amount, "{}", userId));
    }

    private void runConcurrently(int requestCount, Runnable command) throws Exception {
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(requestCount)) {
            List<? extends Future<?>> futures = IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        await(start);
                        command.run();
                        return null;
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("동시 실행 시작 신호를 기다리는 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("동시 실행 대기가 중단되었습니다.", exception);
        }
    }
}
