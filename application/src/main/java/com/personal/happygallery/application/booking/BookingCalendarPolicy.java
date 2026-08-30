package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.BookingCalendarSettingsPort;
import com.personal.happygallery.application.booking.port.out.BookingDayOverridePort;
import com.personal.happygallery.application.booking.port.out.BookingTimeBlockPort;
import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import com.personal.happygallery.domain.booking.BookingDayOverride;
import com.personal.happygallery.domain.booking.BookingTimeBlock;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
class BookingCalendarPolicy {

    private final BookingCalendarSettingsPort settingsPort;
    private final BookingDayOverridePort dayOverridePort;
    private final BookingTimeBlockPort timeBlockPort;
    private final KoreanPublicHolidayPolicy publicHolidayPolicy;

    BookingCalendarPolicy(BookingCalendarSettingsPort settingsPort,
                          BookingDayOverridePort dayOverridePort,
                          BookingTimeBlockPort timeBlockPort,
                          KoreanPublicHolidayPolicy publicHolidayPolicy) {
        this.settingsPort = settingsPort;
        this.dayOverridePort = dayOverridePort;
        this.timeBlockPort = timeBlockPort;
        this.publicHolidayPolicy = publicHolidayPolicy;
    }

    CalendarRules rules(LocalDate dateFrom, LocalDate dateTo) {
        BookingCalendarSettings settings = settingsPort.findById(BookingCalendarSettings.SINGLETON_ID)
                .orElseThrow(NotFoundException.supplier("예약 캘린더 설정"));
        Map<LocalDate, BookingDayOverride> overrides = dayOverridePort
                .findByDateBetweenOrderByDate(dateFrom, dateTo).stream()
                .collect(Collectors.toMap(BookingDayOverride::getDate, Function.identity()));
        Map<LocalDate, List<BookingTimeBlock>> blocks = timeBlockPort
                .findByDateBetweenOrderByDateAscStartTimeAsc(dateFrom, dateTo).stream()
                .collect(Collectors.groupingBy(BookingTimeBlock::getDate));
        return new CalendarRules(settings, overrides, blocks);
    }

    List<LocalDateTime> availableStarts(BookingClass bookingClass,
                                        LocalDate dateFrom,
                                        LocalDate dateTo,
                                        LocalDateTime now,
                                        CalendarRules rules) {
        return dateFrom.datesUntil(dateTo.plusDays(1))
                .flatMap(date -> startsForDate(bookingClass, date, rules, now).stream())
                .toList();
    }

    boolean isAvailable(LocalDateTime startAt,
                        LocalDateTime endAt,
                        CalendarRules rules) {
        return rules.isAvailable(startAt, endAt, publicHolidayPolicy);
    }

    boolean isPublicHoliday(LocalDate date) {
        return publicHolidayPolicy.isPublicHoliday(date);
    }

    BookingDayAvailability effectiveAvailability(LocalDate date, CalendarRules rules) {
        return rules.effectiveAvailability(date, publicHolidayPolicy);
    }

    private List<LocalDateTime> startsForDate(BookingClass bookingClass,
                                              LocalDate date,
                                              CalendarRules rules,
                                              LocalDateTime now) {
        BookingCalendarSettings settings = rules.settings();
        LocalTime startTime = settings.getOpenTime();
        LocalDateTime closeAt = date.atTime(settings.getCloseTime());
        return Stream.iterate(
                        startTime,
                        time -> time.isBefore(settings.getCloseTime()),
                        time -> time.plusMinutes(settings.getSlotIntervalMin()))
                .map(date::atTime)
                .filter(startAt -> startAt.isAfter(now))
                .filter(startAt -> {
                    LocalDateTime endAt = startAt.plusMinutes(bookingClass.getDurationMin());
                    return !endAt.isAfter(closeAt)
                            && rules.isAvailable(startAt, endAt, publicHolidayPolicy);
                })
                .toList();
    }

    record CalendarRules(
            BookingCalendarSettings settings,
            Map<LocalDate, BookingDayOverride> overrides,
            Map<LocalDate, List<BookingTimeBlock>> blocks
    ) {
        boolean isAvailable(LocalDateTime startAt,
                            LocalDateTime endAt,
                            KoreanPublicHolidayPolicy holidayPolicy) {
            LocalDate date = startAt.toLocalDate();
            if (!endAt.toLocalDate().equals(date)
                    || startAt.toLocalTime().isBefore(settings.getOpenTime())
                    || endAt.toLocalTime().isAfter(settings.getCloseTime())) {
                return false;
            }
            long elapsedMinutes = Duration.between(
                    settings.getOpenTime(), startAt.toLocalTime()).toMinutes();
            if (!settings.getOpenTime().plusMinutes(elapsedMinutes)
                    .equals(startAt.toLocalTime())
                    || elapsedMinutes % settings.getSlotIntervalMin() != 0) {
                return false;
            }
            BookingDayOverride override = overrides.get(date);
            if (override != null && override.getAvailability() == BookingDayAvailability.CLOSED) {
                return false;
            }
            if (override == null
                    && settings.isBlockPublicHolidays()
                    && holidayPolicy.isPublicHoliday(date)) {
                return false;
            }
            return blocks.getOrDefault(date, List.of()).stream()
                    .noneMatch(block -> block.overlaps(startAt.toLocalTime(), endAt.toLocalTime()));
        }

        BookingDayAvailability effectiveAvailability(
                LocalDate date,
                KoreanPublicHolidayPolicy holidayPolicy) {
            BookingDayOverride override = overrides.get(date);
            if (override != null) return override.getAvailability();
            return settings.isBlockPublicHolidays() && holidayPolicy.isPublicHoliday(date)
                    ? BookingDayAvailability.CLOSED
                    : BookingDayAvailability.OPEN;
        }
    }
}
