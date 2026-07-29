package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.GuestRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotCapacity;
import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] 슬롯 정원 동시 예약 동시성 검증.
 *
 * <p>Proof (§12.1 DoD): 여석 1명 남은 슬롯에 3개 스레드가 동시에 예약할 때
 * PESSIMISTIC_WRITE 락으로 정확히 1건만 성공하고 나머지는 정원 초과로 실패한다.
 */
@UseCaseIT
class ConcurrentBookingUseCaseIT {

    @Autowired SlotCapacitySupport slotCapacitySupport;
    @Autowired SlotManagementUseCase slotManagementUseCase;
    @Autowired ClassRepository classRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired GuestPersonalDataProtector guestPersonalDataProtector;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;

    private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime SLOT_END   = LocalDateTime.of(2026, 6, 1, 12, 0);

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // Proof (§12.1 ★★★): 여석 1명 → 동시 3건 중 1건만 성공
    // -----------------------------------------------------------------------

    @DisplayName("남은 자리 1개에서 동시 예약을 시도하면 1건만 성공한다")
    @Test
    void concurrentBooking_oneSpotLeft_onlyOneSucceeds() throws InterruptedException {
        BookingClass cls = classRepository.save(
                bookingClass("동시성 테스트 클래스", "CONCURRENCY", 120, 50_000L, 0));
        Slot slot = slotRepository.save(slot(cls, SLOT_START, SLOT_END));

        // 슬롯을 MAX-1 상태로 채움
        for (int i = 0; i < SlotCapacity.MAX - 1; i++) {
            reserveCapacityInTx(slot.getId());
        }
        int beforeRaceBookedCount = slotRepository.findById(slot.getId()).orElseThrow().getBookedCount();
        assertThat(beforeRaceBookedCount).isEqualTo(SlotCapacity.MAX - 1);

        int threadCount = 3;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures  = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    reserveCapacityInTx(slot.getId());
                    successes.incrementAndGet();
                } catch (CapacityExceededException e) {
                    failures.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        startLatch.countDown();
        exec.shutdown();
        exec.awaitTermination(15, TimeUnit.SECONDS);

        // 최종 bookedCount == MAX (초과 없음)
        int bookedCount = slotRepository.findById(slot.getId()).orElseThrow().getBookedCount();
        assertSoftly(softly -> {
            softly.assertThat(successes.get()).isEqualTo(1);
            softly.assertThat(failures.get()).isEqualTo(threadCount - 1);
            softly.assertThat(bookedCount).isEqualTo(SlotCapacity.MAX);
        });
    }

    @DisplayName("버퍼가 충돌하는 앞뒤 슬롯을 동시에 예약하면 한쪽만 성공한다")
    @Test
    void concurrentBooking_reverseBufferConflict_onlyOneSucceeds() throws Exception {
        BookingClass cls = classRepository.save(
                bookingClass("버퍼 동시성 테스트 클래스", "CONCURRENCY", 120, 50_000L, 30));
        Slot frontSlot = slotRepository.save(slot(cls, SLOT_START, SLOT_END));
        Slot rearSlot = slotRepository.save(
                slot(cls, SLOT_END.plusMinutes(15), SLOT_END.plusMinutes(135)));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            Future<Boolean> frontResult = executor.submit(
                    () -> reserveIfAvailable(frontSlot.getId(), startLatch));
            Future<Boolean> rearResult = executor.submit(
                    () -> reserveIfAvailable(rearSlot.getId(), startLatch));

            startLatch.countDown();
            boolean frontSucceeded = frontResult.get(15, TimeUnit.SECONDS);
            boolean rearSucceeded = rearResult.get(15, TimeUnit.SECONDS);

            int frontBookedCount = slotRepository.findById(frontSlot.getId()).orElseThrow().getBookedCount();
            int rearBookedCount = slotRepository.findById(rearSlot.getId()).orElseThrow().getBookedCount();
            assertSoftly(softly -> {
                softly.assertThat(frontSucceeded).isNotEqualTo(rearSucceeded);
                softly.assertThat(frontBookedCount + rearBookedCount).isEqualTo(1);
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @DisplayName("예약 확정과 버퍼 슬롯 생성을 동시에 실행해도 새 슬롯은 차단된다")
    @Test
    void concurrentBookingAndBufferSlotCreation_createdSlotReflectsBooking() throws Exception {
        BookingClass cls = classRepository.save(
                bookingClass("슬롯 생성 동시성 테스트 클래스", "CONCURRENCY", 120, 50_000L, 30));
        Slot sourceSlot = slotRepository.save(slot(cls, SLOT_START, SLOT_END));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            Future<?> bookingResult = executor.submit(() -> {
                startLatch.await();
                reserveCapacityInTx(sourceSlot.getId());
                return null;
            });
            Future<Slot> creationResult = executor.submit(() -> {
                startLatch.await();
                return slotManagementUseCase.createSlot(
                        cls.getId(), SLOT_END.plusMinutes(15));
            });

            startLatch.countDown();
            bookingResult.get(15, TimeUnit.SECONDS);
            Slot createdSlot = creationResult.get(15, TimeUnit.SECONDS);

            Slot persistedSource = slotRepository.findById(sourceSlot.getId()).orElseThrow();
            Slot persistedCreated = slotRepository.findById(createdSlot.getId()).orElseThrow();
            assertSoftly(softly -> {
                softly.assertThat(persistedSource.getBookedCount()).isEqualTo(1);
                softly.assertThat(persistedCreated.isBufferBlocked()).isTrue();
                softly.assertThat(persistedCreated.isActive()).isFalse();
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @DisplayName("같은 전화번호의 회원과 비회원이 동시에 예약해도 한 건만 저장한다")
    @Test
    void concurrentMemberAndGuestBooking_samePhone_onlyOneSucceeds() throws Exception {
        String phone = "01077778888";
        BookingClass cls = classRepository.save(
                bookingClass("예약자 식별 동시성 클래스", "OWNER_CONCURRENCY", 120, 50_000L, 0));
        Slot slot = slotRepository.save(slot(cls, SLOT_START, SLOT_END));
        User user = userStorePort.save(
                new User("owner-concurrency@test.local", "password-hash", "회원 예약자", phone));
        Guest guest = guestRepository.save(
                guestPersonalDataProtector.newGuest("비회원 예약자", phone));
        assertThat(user.getPhoneHmac()).isEqualTo(guest.getPhoneHmac());

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> memberResult = executor.submit(
                    () -> createMemberBooking(slot.getId(), user, startLatch));
            Future<Boolean> guestResult = executor.submit(
                    () -> createGuestBooking(slot.getId(), guest, startLatch));

            startLatch.countDown();
            boolean memberSucceeded = memberResult.get(15, TimeUnit.SECONDS);
            boolean guestSucceeded = guestResult.get(15, TimeUnit.SECONDS);

            assertSoftly(softly -> {
                softly.assertThat(memberSucceeded).isNotEqualTo(guestSucceeded);
                softly.assertThat(bookingRepository.count()).isEqualTo(1L);
                softly.assertThat(slotRepository.findById(slot.getId()).orElseThrow().getBookedCount())
                        .isEqualTo(1);
            });
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean createMemberBooking(Long slotId, User user, CountDownLatch startLatch)
            throws InterruptedException {
        startLatch.await();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Slot reserved = slotCapacitySupport.reserveCapacity(slotId);
                bookingRepository.saveAndFlush(Booking.forMemberDeposit(
                        user, reserved, 5_000L, 45_000L, DepositPaymentMethod.CARD));
            });
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private boolean createGuestBooking(Long slotId, Guest guest, CountDownLatch startLatch)
            throws InterruptedException {
        startLatch.await();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                Slot reserved = slotCapacitySupport.reserveCapacity(slotId);
                bookingRepository.saveAndFlush(Booking.forGuestDeposit(
                        guest, reserved, 5_000L, 45_000L,
                        DepositPaymentMethod.CARD, "concurrent-guest-access-token"));
            });
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }

    private boolean reserveIfAvailable(Long slotId, CountDownLatch startLatch) throws InterruptedException {
        startLatch.await();
        try {
            reserveCapacityInTx(slotId);
            return true;
        } catch (SlotNotAvailableException e) {
            return false;
        }
    }

    private void reserveCapacityInTx(Long slotId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> slotCapacitySupport.reserveCapacity(slotId));
    }
}
