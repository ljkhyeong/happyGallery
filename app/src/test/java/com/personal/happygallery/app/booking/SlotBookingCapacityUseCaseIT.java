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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.TestDataCleaner.clearBookingData;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] 슬롯 정원(8명) 강제 + 뒤쪽 버퍼 비활성화 검증.
 *
 * <p>Proof (docs/PRD/0001_기준_스펙/spec.md §5.1): 같은 슬롯에 9번째 예약 시도는 실패로 귀결.
 */
@UseCaseIT
class SlotBookingCapacityUseCaseIT {

    @Autowired SlotBookingCoordinator slotBookingCoordinator;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingRepository bookingRepository;

    BookingClass bookingClass;
    Slot mainSlot;

    // 슬롯: 10:00~12:00, buffer_min=30 → 버퍼 범위 [12:00, 12:30)
    private static final LocalDateTime MAIN_START  = LocalDateTime.of(2026, 4, 1, 10, 0);
    private static final LocalDateTime MAIN_END    = LocalDateTime.of(2026, 4, 1, 12, 0);
    // 버퍼 범위 안 — 12:00 (inclusive)
    private static final LocalDateTime BUFFER_IN   = LocalDateTime.of(2026, 4, 1, 12, 0);
    // 버퍼 범위 안 — 12:15
    private static final LocalDateTime BUFFER_IN2  = LocalDateTime.of(2026, 4, 1, 12, 15);
    // 버퍼 범위 밖 — 12:30 (exclusive)
    private static final LocalDateTime BUFFER_OUT  = LocalDateTime.of(2026, 4, 1, 12, 30);

    @BeforeEach
    void setUp() {
        clearBookingData(bookingHistoryRepository, bookingRepository, slotRepository, classRepository);
        bookingClass = classRepository.save(defaultBookingClass());
        mainSlot = slotRepository.save(slot(bookingClass, MAIN_START, MAIN_END));
    }

    @DisplayName("슬롯 정원 8명까지 예약 확정은 모두 성공한다")
    @Test
    void confirmBooking_8times_allSucceed() {
        for (int i = 0; i < SlotCapacity.MAX; i++) {
            slotBookingCoordinator.confirmBooking(mainSlot.getId());
        }
        Slot updated = slotRepository.findById(mainSlot.getId()).orElseThrow();
        assertThat(updated.getBookedCount()).isEqualTo(SlotCapacity.MAX);
    }

    @DisplayName("9번째 예약 확정 시 정원 초과 예외가 발생한다")
    @Test
    void confirmBooking_9th_throwsCapacityExceeded() {
        for (int i = 0; i < SlotCapacity.MAX; i++) {
            slotBookingCoordinator.confirmBooking(mainSlot.getId());
        }

        assertThatThrownBy(() -> slotBookingCoordinator.confirmBooking(mainSlot.getId()))
                .isInstanceOf(CapacityExceededException.class);

        // booked_count 변경 없음 확인
        Slot updated = slotRepository.findById(mainSlot.getId()).orElseThrow();
        assertThat(updated.getBookedCount()).isEqualTo(SlotCapacity.MAX);
    }

    @DisplayName("예약 확정 시 버퍼 구간 슬롯이 비활성화된다")
    @Test
    void confirmBooking_deactivatesBufferSlots() {
        Slot bufferSlot1 = slotRepository.save(
                slot(bookingClass, BUFFER_IN, BUFFER_IN.plusHours(2)));
        Slot bufferSlot2 = slotRepository.save(
                slot(bookingClass, BUFFER_IN2, BUFFER_IN2.plusHours(2)));

        slotBookingCoordinator.confirmBooking(mainSlot.getId());

        assertSoftly(softly -> {
            softly.assertThat(slotRepository.findById(bufferSlot1.getId()).orElseThrow().isActive()).isFalse();
            softly.assertThat(slotRepository.findById(bufferSlot2.getId()).orElseThrow().isActive()).isFalse();
        });
    }

    @DisplayName("예약 확정 시 버퍼 외 슬롯은 비활성화되지 않는다")
    @Test
    void confirmBooking_doesNotDeactivateSlotOutsideBuffer() {
        Slot outsideSlot = slotRepository.save(
                slot(bookingClass, BUFFER_OUT, BUFFER_OUT.plusHours(2)));

        slotBookingCoordinator.confirmBooking(mainSlot.getId());

        assertThat(slotRepository.findById(outsideSlot.getId()).orElseThrow().isActive()).isTrue();
    }
}
