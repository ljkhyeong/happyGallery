package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.CapacityExceededException;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotCapacity;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [UseCaseIT] 슬롯 정원 동시 예약 동시성 검증.
 *
 * <p>Proof (§12.1 DoD): 여석 1명 남은 슬롯에 3개 스레드가 동시에 예약할 때
 * PESSIMISTIC_WRITE 락으로 정확히 1건만 성공하고 나머지는 정원 초과로 실패한다.
 */
@UseCaseIT
class ConcurrentBookingUseCaseIT {

    @Autowired SlotManagementService slotManagementService;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingRepository bookingRepository;

    private static final LocalDateTime SLOT_START = LocalDateTime.of(2026, 6, 1, 10, 0);
    private static final LocalDateTime SLOT_END   = LocalDateTime.of(2026, 6, 1, 12, 0);

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
    }

    // -----------------------------------------------------------------------
    // Proof (§12.1 ★★★): 여석 1명 → 동시 3건 중 1건만 성공
    // -----------------------------------------------------------------------

    @DisplayName("남은 자리 1개에서 동시 예약을 시도하면 1건만 성공한다")
    @Test
    void concurrentBooking_oneSpotLeft_onlyOneSucceeds() throws InterruptedException {
        BookingClass cls = classRepository.save(
                new BookingClass("동시성 테스트 클래스", "CONCURRENCY", 120, 50_000L, 0));
        Slot slot = slotRepository.save(new Slot(cls, SLOT_START, SLOT_END));

        // 슬롯을 MAX-1 상태로 채움
        for (int i = 0; i < SlotCapacity.MAX - 1; i++) {
            slotManagementService.confirmBooking(slot.getId());
        }
        assertThat(slotRepository.findById(slot.getId()).orElseThrow().getBookedCount())
                .isEqualTo(SlotCapacity.MAX - 1);

        int threadCount = 3;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures  = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    slotManagementService.confirmBooking(slot.getId());
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

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(threadCount - 1);

        // 최종 bookedCount == MAX (초과 없음)
        int bookedCount = slotRepository.findById(slot.getId()).orElseThrow().getBookedCount();
        assertThat(bookedCount).isEqualTo(SlotCapacity.MAX);
    }
}
