package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import com.personal.happygallery.domain.booking.BookingTimeBlock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 기본 개방 예약 캘린더와 날짜·시간 차단을 관리한다. */
public interface BookingCalendarUseCase {

    enum DayOverrideMode {
        DEFAULT,
        OPEN,
        CLOSED
    }

    record UpdateSettingsCommand(
            long expectedVersion,
            LocalTime openTime,
            LocalTime closeTime,
            int slotIntervalMin,
            boolean blockPublicHolidays
    ) {}

    record UpdateDayCommand(
            LocalDate date,
            DayOverrideMode mode,
            String reason
    ) {}

    record CreateTimeBlockCommand(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String reason
    ) {}

    record CalendarDay(
            LocalDate date,
            boolean publicHoliday,
            BookingDayAvailability effectiveAvailability,
            DayOverrideMode overrideMode,
            String reason,
            List<BookingTimeBlock> timeBlocks
    ) {
        public CalendarDay {
            timeBlocks = List.copyOf(timeBlocks);
        }
    }

    record CalendarView(
            BookingCalendarSettings settings,
            List<CalendarDay> days
    ) {
        public CalendarView {
            days = List.copyOf(days);
        }
    }

    BookingCalendarSettings getSettings();

    BookingCalendarSettings updateSettings(UpdateSettingsCommand command);

    CalendarView getCalendar(LocalDate dateFrom, LocalDate dateTo);

    void updateDay(UpdateDayCommand command);

    BookingTimeBlock createTimeBlock(CreateTimeBlockCommand command);

    void deleteTimeBlock(Long blockId);
}
