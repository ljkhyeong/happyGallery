package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase;
import com.personal.happygallery.application.booking.port.out.BookingCalendarSettingsPort;
import com.personal.happygallery.application.booking.port.out.BookingDayOverridePort;
import com.personal.happygallery.application.booking.port.out.BookingTimeBlockPort;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.domain.booking.BookingCalendarSettings;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingDayAvailability;
import com.personal.happygallery.domain.booking.BookingDayOverride;
import com.personal.happygallery.domain.booking.BookingTimeBlock;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultBookingCalendarService implements BookingCalendarUseCase {

    private static final int MAX_CALENDAR_DAYS = 93;
    private static final int MATERIALIZATION_DAYS = 30;

    private final BookingCalendarSettingsPort settingsPort;
    private final BookingDayOverridePort dayOverridePort;
    private final BookingTimeBlockPort timeBlockPort;
    private final ClassReaderPort classReaderPort;
    private final BookingCalendarPolicy calendarPolicy;
    private final BookingCalendarSlotMaterializer slotMaterializer;
    private final Clock clock;

    public DefaultBookingCalendarService(BookingCalendarSettingsPort settingsPort,
                                         BookingDayOverridePort dayOverridePort,
                                         BookingTimeBlockPort timeBlockPort,
                                         ClassReaderPort classReaderPort,
                                         BookingCalendarPolicy calendarPolicy,
                                         BookingCalendarSlotMaterializer slotMaterializer,
                                         Clock clock) {
        this.settingsPort = settingsPort;
        this.dayOverridePort = dayOverridePort;
        this.timeBlockPort = timeBlockPort;
        this.classReaderPort = classReaderPort;
        this.calendarPolicy = calendarPolicy;
        this.slotMaterializer = slotMaterializer;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingCalendarSettings getSettings() {
        return settingsPort.findById(BookingCalendarSettings.SINGLETON_ID)
                .orElseThrow(NotFoundException.supplier("예약 캘린더 설정"));
    }

    @Override
    public BookingCalendarSettings updateSettings(UpdateSettingsCommand command) {
        List<BookingClass> classes = lockAllClasses();
        BookingCalendarSettings settings = settingsPort
                .findByIdForUpdate(BookingCalendarSettings.SINGLETON_ID)
                .orElseThrow(NotFoundException.supplier("예약 캘린더 설정"));
        if (settings.getVersion() != command.expectedVersion()) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "예약 캘린더 설정이 변경되었습니다. 최신 내용을 다시 불러와 주세요.");
        }
        settings.update(
                command.openTime(),
                command.closeTime(),
                command.slotIntervalMin(),
                command.blockPublicHolidays());
        BookingCalendarSettings saved = settingsPort.save(settings);
        refreshNextMaterializationPeriod(classes);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public CalendarView getCalendar(LocalDate dateFrom, LocalDate dateTo) {
        requireCalendarRange(dateFrom, dateTo);
        BookingCalendarPolicy.CalendarRules rules = calendarPolicy.rules(dateFrom, dateTo);
        List<CalendarDay> days = dateFrom.datesUntil(dateTo.plusDays(1))
                .map(date -> toCalendarDay(date, rules, rules.overrides(), rules.blocks()))
                .toList();
        return new CalendarView(rules.settings(), days);
    }

    @Override
    public void updateDay(UpdateDayCommand command) {
        requireEditableDate(command.date());
        List<BookingClass> classes = lockAllClasses();
        if (command.mode() == DayOverrideMode.DEFAULT) {
            dayOverridePort.deleteById(command.date());
        } else {
            BookingDayAvailability availability = command.mode() == DayOverrideMode.OPEN
                    ? BookingDayAvailability.OPEN
                    : BookingDayAvailability.CLOSED;
            dayOverridePort.save(new BookingDayOverride(
                    command.date(), availability, command.reason()));
        }
        refreshDate(classes, command.date());
    }

    @Override
    public BookingTimeBlock createTimeBlock(CreateTimeBlockCommand command) {
        requireEditableDate(command.date());
        List<BookingClass> classes = lockAllClasses();
        if (timeBlockPort.existsByDateAndStartTimeAndEndTime(
                command.date(), command.startTime(), command.endTime())) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "같은 예약 차단 시간이 이미 등록되어 있습니다.");
        }
        BookingTimeBlock block = timeBlockPort.save(new BookingTimeBlock(
                command.date(), command.startTime(), command.endTime(), command.reason()));
        refreshDate(classes, command.date());
        return block;
    }

    @Override
    public void deleteTimeBlock(Long blockId) {
        List<BookingClass> classes = lockAllClasses();
        BookingTimeBlock block = timeBlockPort.findById(blockId)
                .orElseThrow(NotFoundException.supplier("예약 차단 시간"));
        timeBlockPort.delete(block);
        refreshDate(classes, block.getDate());
    }

    private CalendarDay toCalendarDay(
            LocalDate date,
            BookingCalendarPolicy.CalendarRules rules,
            Map<LocalDate, BookingDayOverride> overrides,
            Map<LocalDate, List<BookingTimeBlock>> blocks) {
        BookingDayOverride override = overrides.get(date);
        DayOverrideMode overrideMode = override == null
                ? DayOverrideMode.DEFAULT
                : override.getAvailability() == BookingDayAvailability.OPEN
                        ? DayOverrideMode.OPEN
                        : DayOverrideMode.CLOSED;
        return new CalendarDay(
                date,
                calendarPolicy.isPublicHoliday(date),
                calendarPolicy.effectiveAvailability(date, rules),
                overrideMode,
                override == null ? null : override.getReason(),
                blocks.getOrDefault(date, List.of()));
    }

    private List<BookingClass> lockAllClasses() {
        List<Long> ids = classReaderPort.findAll().stream()
                .map(BookingClass::getId)
                .sorted()
                .toList();
        if (ids.isEmpty()) return List.of();
        return classReaderPort.findAllByIdForUpdate(ids);
    }

    private void refreshNextMaterializationPeriod(List<BookingClass> classes) {
        LocalDate start = LocalDate.now(clock);
        LocalDateTime rangeStart = start.atStartOfDay();
        LocalDateTime rangeEnd = start.plusDays(MATERIALIZATION_DAYS).atStartOfDay();
        classes.forEach(bookingClass ->
                slotMaterializer.refreshExisting(bookingClass, rangeStart, rangeEnd));
    }

    private void refreshDate(List<BookingClass> classes, LocalDate date) {
        LocalDateTime rangeStart = date.atStartOfDay();
        LocalDateTime rangeEnd = date.plusDays(1).atStartOfDay();
        classes.forEach(bookingClass ->
                slotMaterializer.refreshExisting(bookingClass, rangeStart, rangeEnd));
    }

    private void requireEditableDate(LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now(clock))) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "오늘 이후 날짜만 예약 캘린더에서 변경할 수 있습니다.");
        }
    }

    private static void requireCalendarRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null || dateFrom.isAfter(dateTo)
                || dateFrom.plusDays(MAX_CALENDAR_DAYS - 1L).isBefore(dateTo)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "예약 캘린더는 한 번에 93일까지 조회할 수 있습니다.");
        }
    }
}
