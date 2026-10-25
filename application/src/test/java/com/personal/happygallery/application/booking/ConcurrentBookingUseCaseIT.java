package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.GuestRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
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
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.ConcurrentExecution.runSimultaneously;
import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static java.util.Collections.nCopies;
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
    @Autowired SlotQueryUseCase slotQueryUseCase;
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
        cleanupSupport.clearBookingDomainData();
    }

    // -----------------------------------------------------------------------
    // Proof (§12.1 ★★★): 여석 1명 → 동시 3건 중 1건만 성공
    // -----------------------------------------------------------------------

    @DisplayName("남은 자리 1개에서 동시 예약을 시도하면 1건만 성공한다")
    @Test
    void concurrentBooking_oneSpotLeft_onlyOneSucceeds() throws Exception {
        BookingClass cls = classRepository.save(
                bookingClass("동시성 테스트 클래스", "CONCURRENCY", 120, 50_000L, 0));
        Slot slot = slotRepository.save(slot(cls, SLOT_START, SLOT_END));

        // 슬롯을 MAX-1 상태로 채움
        for (int i = 0; i < SlotCapacity.DEFAULT - 1; i++) {
            reserveCapacityInTx(slot.getId());
        }
        int beforeRaceBookedCount = slotRepository.findById(slot.getId()).orElseThrow().getBookedCount();
        assertThat(beforeRaceBookedCount).isEqualTo(SlotCapacity.DEFAULT - 1);

        int threadCount = 3;
        List<Throwable> outcomes = runSimultaneously(
                nCopies(threadCount, () -> reserveCapacityInTx(slot.getId())));

        // 최종 bookedCount == MAX (초과 없음)
        int bookedCount = slotRepository.findById(slot.getId()).orElseThrow().getBookedCount();
        assertSoftly(softly -> {
            softly.assertThat(outcomes).filteredOn(Objects::isNull).hasSize(1);
            softly.assertThat(outcomes).filteredOn(Objects::nonNull)
                    .hasSize(threadCount - 1)
                    .allMatch(CapacityExceededException.class::isInstance);
            softly.assertThat(bookedCount).isEqualTo(SlotCapacity.DEFAULT);
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

        List<Throwable> outcomes = runSimultaneously(List.of(
                () -> reserveCapacityInTx(frontSlot.getId()),
                () -> reserveCapacityInTx(rearSlot.getId())));

        int frontBookedCount = slotRepository.findById(frontSlot.getId()).orElseThrow().getBookedCount();
        int rearBookedCount = slotRepository.findById(rearSlot.getId()).orElseThrow().getBookedCount();
        assertSoftly(softly -> {
            softly.assertThat(outcomes).filteredOn(Objects::isNull).hasSize(1);
            softly.assertThat(outcomes).filteredOn(Objects::nonNull)
                    .singleElement()
                    .isInstanceOf(SlotNotAvailableException.class);
            softly.assertThat(frontBookedCount + rearBookedCount).isEqualTo(1);
        });
    }

    @DisplayName("예약 확정과 캘린더 회차 자동 준비를 동시에 실행해도 새 회차는 차단된다")
    @Test
    void concurrentBookingAndCalendarMaterialization_createdSlotReflectsBooking() throws Exception {
        BookingClass cls = classRepository.save(
                bookingClass("슬롯 생성 동시성 테스트 클래스", "CONCURRENCY", 120, 50_000L, 30));
        Slot sourceSlot = slotRepository.save(slot(cls, SLOT_START, SLOT_END));

        assertThat(runSimultaneously(List.of(
                () -> reserveCapacityInTx(sourceSlot.getId()),
                () -> slotQueryUseCase.listAvailable(cls.getId(), SLOT_START.toLocalDate()))))
                .containsOnlyNulls();

        Slot createdSlot = slotRepository.findByBookingClassIdOrderByStartAtDesc(cls.getId()).stream()
                .filter(candidate -> candidate.getStartAt().equals(SLOT_END))
                .findFirst()
                .orElseThrow();

        Slot persistedSource = slotRepository.findById(sourceSlot.getId()).orElseThrow();
        Slot persistedCreated = slotRepository.findById(createdSlot.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(persistedSource.getBookedCount()).isEqualTo(1);
            softly.assertThat(persistedCreated.isBufferBlocked()).isTrue();
            softly.assertThat(persistedCreated.isActive()).isFalse();
        });
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

        List<Throwable> outcomes = runSimultaneously(List.of(
                () -> createMemberBooking(slot.getId(), user),
                () -> createGuestBooking(slot.getId(), guest)));

        assertSoftly(softly -> {
            softly.assertThat(outcomes).filteredOn(Objects::isNull).hasSize(1);
            softly.assertThat(outcomes).filteredOn(Objects::nonNull)
                    .singleElement()
                    .isInstanceOf(DataIntegrityViolationException.class);
            softly.assertThat(bookingRepository.count()).isEqualTo(1L);
            softly.assertThat(slotRepository.findById(slot.getId()).orElseThrow().getBookedCount())
                    .isEqualTo(1);
        });
    }

    private void createMemberBooking(Long slotId, User user) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Slot reserved = slotCapacitySupport.reserveCapacity(slotId, 1);
            bookingRepository.saveAndFlush(Booking.forMemberDeposit(
                    user, reserved, 1, 5_000L, 45_000L, DepositPaymentMethod.CARD));
        });
    }

    private void createGuestBooking(Long slotId, Guest guest) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Slot reserved = slotCapacitySupport.reserveCapacity(slotId, 1);
            bookingRepository.saveAndFlush(Booking.forGuestDeposit(
                    guest, reserved, 1, 5_000L, 45_000L,
                    DepositPaymentMethod.CARD, "concurrent-guest-access-token"));
        });
    }

    private void reserveCapacityInTx(Long slotId) {
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> slotCapacitySupport.reserveCapacity(slotId, 1));
    }
}
