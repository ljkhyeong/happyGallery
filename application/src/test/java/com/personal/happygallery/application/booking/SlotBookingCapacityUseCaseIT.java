package com.personal.happygallery.application.booking;

import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotCapacity;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] 슬롯 정원(8명) 강제 + 뒤쪽 버퍼 차단·자동 해제 검증.
 *
 * <p>Proof (docs/PRD/0001_기준_스펙/spec.md §4.1): 같은 슬롯에 9번째 예약 시도는 실패로 귀결.
 */
@UseCaseIT
class SlotBookingCapacityUseCaseIT {

    @Autowired SlotCapacitySupport slotCapacitySupport;
    @Autowired DefaultSlotQueryService slotQueryService;
    @Autowired DefaultSlotManagementService slotManagementService;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired Clock clock;

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
        bookingClass = classRepository.save(defaultBookingClass());
        mainSlot = slotRepository.save(slot(bookingClass, MAIN_START, MAIN_END));
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingData();
    }

    @DisplayName("슬롯 정원 8명까지 예약 확정은 모두 성공한다")
    @Test
    void reserveCapacity_8times_allSucceed() {
        for (int i = 0; i < SlotCapacity.MAX; i++) {
            reserveCapacityInTx(mainSlot.getId());
        }
        Slot updated = slotRepository.findById(mainSlot.getId()).orElseThrow();
        assertThat(updated.getBookedCount()).isEqualTo(SlotCapacity.MAX);
    }

    @DisplayName("9번째 예약 확정 시 정원 초과 예외가 발생한다")
    @Test
    void reserveCapacity_9th_throwsCapacityExceeded() {
        for (int i = 0; i < SlotCapacity.MAX; i++) {
            reserveCapacityInTx(mainSlot.getId());
        }

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> reserveCapacityInTx(mainSlot.getId()))
                    .isInstanceOf(CapacityExceededException.class);

            // booked_count 변경 없음 확인
            Slot updated = slotRepository.findById(mainSlot.getId()).orElseThrow();
            softly.assertThat(updated.getBookedCount()).isEqualTo(SlotCapacity.MAX);
        });
    }

    @DisplayName("예약 확정 시 버퍼 구간 슬롯이 차단된다")
    @Test
    void reserveCapacity_blocksBufferSlots() {
        Slot bufferSlot1 = slotRepository.save(
                slot(bookingClass, BUFFER_IN, BUFFER_IN.plusHours(2)));
        Slot bufferSlot2 = slotRepository.save(
                slot(bookingClass, BUFFER_IN2, BUFFER_IN2.plusHours(2)));

        reserveCapacityInTx(mainSlot.getId());

        assertSoftly(softly -> {
            softly.assertThat(slotRepository.findById(bufferSlot1.getId()).orElseThrow().isActive()).isFalse();
            softly.assertThat(slotRepository.findById(bufferSlot2.getId()).orElseThrow().isActive()).isFalse();
        });
    }

    @DisplayName("예약 확정 시 버퍼 외 슬롯은 차단되지 않는다")
    @Test
    void reserveCapacity_doesNotBlockSlotOutsideBuffer() {
        Slot outsideSlot = slotRepository.save(
                slot(bookingClass, BUFFER_OUT, BUFFER_OUT.plusHours(2)));

        reserveCapacityInTx(mainSlot.getId());

        assertThat(slotRepository.findById(outsideSlot.getId()).orElseThrow().isActive()).isTrue();
    }

    @DisplayName("공개 조회는 이미 시작한 슬롯을 제외하고 미래 슬롯만 반환한다")
    @Test
    void listAvailable_excludesStartedSlots() {
        LocalDateTime now = LocalDateTime.now(clock);
        Slot pastSlot = slotRepository.save(
                slot(bookingClass, now.minusHours(2), now.minusHours(1)));
        Slot startingSlot = slotRepository.save(
                slot(bookingClass, now, now.plusHours(2)));
        Slot futureSlot = slotRepository.save(
                slot(bookingClass, now.plusMinutes(1), now.plusHours(2).plusMinutes(1)));

        List<Long> availableSlotIds = slotQueryService.listAvailable(
                        bookingClass.getId(), now.toLocalDate()).stream()
                .map(Slot::getId)
                .toList();

        assertSoftly(softly -> {
            softly.assertThat(availableSlotIds).contains(futureSlot.getId());
            softly.assertThat(availableSlotIds).doesNotContain(pastSlot.getId(), startingSlot.getId());
        });
    }

    @DisplayName("향후 슬롯 조회는 오늘부터 지정 기간 안의 예약 가능 슬롯만 반환한다")
    @Test
    void listUpcoming_returnsOnlyAvailableSlotsInsideRange() {
        LocalDateTime now = LocalDateTime.now(clock);
        Slot withinRange = slotRepository.save(
                slot(bookingClass, now.plusDays(1), now.plusDays(1).plusHours(2)));
        Slot atRangeEnd = slotRepository.save(
                slot(bookingClass, now.toLocalDate().plusDays(14).atStartOfDay(),
                        now.toLocalDate().plusDays(14).atTime(2, 0)));

        List<Long> availableSlotIds = slotQueryService.listUpcoming(bookingClass.getId(), 14).stream()
                .map(Slot::getId)
                .toList();

        assertSoftly(softly -> {
            softly.assertThat(availableSlotIds).contains(withinRange.getId());
            softly.assertThat(availableSlotIds).doesNotContain(atRangeEnd.getId());
        });
    }

    @DisplayName("예약 확정 시 슬롯 시작 시각이 되면 잠금 후 검증에서 거절한다")
    @Test
    void reserveCapacity_rejectsSlotStartingNow() {
        LocalDateTime now = LocalDateTime.now(clock);
        Slot startingSlot = slotRepository.save(slot(bookingClass, now, now.plusHours(2)));

        assertThatThrownBy(() -> reserveCapacityInTx(startingSlot.getId()))
                .isInstanceOf(SlotNotAvailableException.class);
        assertThat(slotRepository.findById(startingSlot.getId()).orElseThrow().getBookedCount()).isZero();
    }

    @DisplayName("뒤쪽 버퍼 슬롯에 예약이 있으면 앞 슬롯의 새 예약을 거절한다")
    @Test
    void reserveCapacity_rejectsReverseOrderBufferConflict() {
        Slot bookedBufferSlot = slotRepository.save(
                slot(bookingClass, BUFFER_IN2, BUFFER_IN2.plusHours(2)));
        reserveCapacityInTx(bookedBufferSlot.getId());

        assertThatThrownBy(() -> reserveCapacityInTx(mainSlot.getId()))
                .isInstanceOf(SlotNotAvailableException.class);

        assertSoftly(softly -> {
            softly.assertThat(slotRepository.findById(mainSlot.getId()).orElseThrow().getBookedCount()).isZero();
            softly.assertThat(slotRepository.findById(bookedBufferSlot.getId()).orElseThrow().getBookedCount())
                    .isEqualTo(1);
        });
    }

    @DisplayName("마지막 예약을 반납하면 버퍼 슬롯만 자동 활성화되고 관리자 비활성 상태는 유지된다")
    @Test
    void releaseLastCapacity_reactivatesOnlyBufferBlockedSlot() {
        Slot bufferSlot = slotRepository.save(
                slot(bookingClass, BUFFER_IN, BUFFER_IN.plusHours(2)));
        Slot manuallyDeactivatedSlot = slotRepository.save(
                slot(bookingClass, BUFFER_IN2, BUFFER_IN2.plusHours(2)));
        manuallyDeactivatedSlot.deactivate();
        slotRepository.save(manuallyDeactivatedSlot);

        reserveCapacityInTx(mainSlot.getId());
        reserveCapacityInTx(mainSlot.getId());

        releaseCapacityInTx(mainSlot.getId());
        assertThat(slotRepository.findById(bufferSlot.getId()).orElseThrow().isActive()).isFalse();

        releaseCapacityInTx(mainSlot.getId());

        var availableSlotIds = slotQueryService.listAvailable(bookingClass.getId(), MAIN_START.toLocalDate())
                .stream()
                .map(Slot::getId)
                .toList();
        assertSoftly(softly -> {
            softly.assertThat(slotRepository.findById(bufferSlot.getId()).orElseThrow().isActive()).isTrue();
            softly.assertThat(slotRepository.findById(manuallyDeactivatedSlot.getId()).orElseThrow().isActive())
                    .isFalse();
            softly.assertThat(availableSlotIds).contains(bufferSlot.getId());
            softly.assertThat(availableSlotIds).doesNotContain(manuallyDeactivatedSlot.getId());
        });
    }

    @DisplayName("여러 예약 슬롯의 버퍼가 겹치면 모든 원인 예약이 사라진 뒤 자동 활성화된다")
    @Test
    void overlappingBufferBlocks_reactivateAfterAllSourceBookingsReleased() {
        Slot secondSourceSlot = slotRepository.save(
                slot(bookingClass, MAIN_START.plusMinutes(15), MAIN_END.plusMinutes(15)));

        reserveCapacityInTx(mainSlot.getId());
        Slot targetSlot = slotManagementService.createSlot(
                bookingClass.getId(), BUFFER_IN2);
        assertThat(targetSlot.isActive()).isFalse();

        reserveCapacityInTx(secondSourceSlot.getId());
        releaseCapacityInTx(mainSlot.getId());
        assertThat(slotRepository.findById(targetSlot.getId()).orElseThrow().isActive()).isFalse();

        releaseCapacityInTx(secondSourceSlot.getId());
        assertThat(slotRepository.findById(targetSlot.getId()).orElseThrow().isActive()).isTrue();
    }

    private void reserveCapacityInTx(Long slotId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> slotCapacitySupport.reserveCapacity(slotId));
    }

    private void releaseCapacityInTx(Long slotId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> slotCapacitySupport.releaseCapacity(slotId));
    }
}
