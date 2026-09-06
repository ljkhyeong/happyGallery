package com.personal.happygallery.application.booking;

import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.DayOverrideMode;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.UpdateDayCommand;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;

@UseCaseIT
@Transactional
class BookingCalendarUseCaseIT {

    @Autowired BookingCalendarUseCase bookingCalendarUseCase;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired EntityManager entityManager;
    @Autowired Clock clock;

    @Test
    @DisplayName("휴무일 변경은 판매 상태와 관계없이 모든 클래스의 기존 슬롯에 반영된다")
    void closeDay_updatesExistingSlotsForAllClasses() {
        BookingClass active = classRepository.save(bookingClass("판매 클래스", "CALENDAR", 60, 50_000L, 0));
        BookingClass inactive = bookingClass("판매 중지 클래스", "CALENDAR", 60, 50_000L, 0);
        inactive.changeStatus(BookingClassStatus.INACTIVE);
        classRepository.save(inactive);
        LocalDateTime startAt = LocalDate.now(clock).plusDays(1).atTime(10, 0);
        List<Long> slotIds = slotRepository.saveAll(List.of(
                slot(active, startAt, startAt.plusHours(1)),
                slot(inactive, startAt, startAt.plusHours(1))))
                .stream().map(Slot::getId).toList();
        entityManager.flush();
        entityManager.clear();

        bookingCalendarUseCase.updateDay(new UpdateDayCommand(
                startAt.toLocalDate(), DayOverrideMode.CLOSED, "공방 휴무"));
        entityManager.flush();
        entityManager.clear();

        assertThat(slotRepository.findAllById(slotIds))
                .hasSize(2)
                .allSatisfy(slot -> assertThat(slot.isCalendarActive()).isFalse());
    }

    @Test
    @DisplayName("등록된 클래스가 없어도 캘린더 휴무일을 설정할 수 있다")
    void closeDay_withoutClassesSavesCalendarOverride() {
        assertThat(classRepository.count()).isZero();
        LocalDate date = LocalDate.now(clock).plusDays(1);

        bookingCalendarUseCase.updateDay(new UpdateDayCommand(date, DayOverrideMode.CLOSED, "공방 휴무"));

        assertThat(bookingCalendarUseCase.getCalendar(date, date).days()).singleElement()
                .extracting(BookingCalendarUseCase.CalendarDay::effectiveAvailability)
                .isEqualTo(BookingDayAvailability.CLOSED);
    }
}
