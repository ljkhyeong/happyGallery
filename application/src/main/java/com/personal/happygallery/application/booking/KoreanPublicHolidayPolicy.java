package com.personal.happygallery.application.booking;

import com.github.usingsky.calendar.KoreanLunarCalendar;
import com.personal.happygallery.application.booking.port.out.PublicHolidaySnapshotPort;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** 공식 공휴일 스냅샷을 우선 사용하고, 스냅샷이 없을 때만 내장 규칙으로 계산한다. */
@Component
class KoreanPublicHolidayPolicy {

    private final PublicHolidaySnapshotPort snapshotPort;
    private final Map<Integer, Set<LocalDate>> holidaysByYear = new ConcurrentHashMap<>();

    KoreanPublicHolidayPolicy(PublicHolidaySnapshotPort snapshotPort) {
        this.snapshotPort = snapshotPort;
    }

    boolean isPublicHoliday(LocalDate date) {
        return holidaysByYear.computeIfAbsent(date.getYear(), this::load).contains(date);
    }

    void evict(int year) {
        holidaysByYear.remove(year);
    }

    private Set<LocalDate> load(int year) {
        Set<LocalDate> officialDates = snapshotPort.findDatesByYear(year);
        return officialDates.isEmpty() ? calculate(year) : Set.copyOf(officialDates);
    }

    private Set<LocalDate> calculate(int year) {
        List<LocalDate> baseDates = new ArrayList<>();
        baseDates.add(LocalDate.of(year, 1, 1));

        LocalDate lunarNewYear = lunarToSolar(year, 1, 1);
        List<LocalDate> seollal = List.of(
                lunarNewYear.minusDays(1), lunarNewYear, lunarNewYear.plusDays(1));
        baseDates.addAll(seollal);

        List<LocalDate> nationalHolidays = new ArrayList<>(List.of(
                LocalDate.of(year, 3, 1),
                LocalDate.of(year, 8, 15),
                LocalDate.of(year, 10, 3),
                LocalDate.of(year, 10, 9)));
        if (year >= 2026) nationalHolidays.add(LocalDate.of(year, 7, 17));
        baseDates.addAll(nationalHolidays);

        LocalDate buddhasBirthday = lunarToSolar(year, 4, 8);
        baseDates.add(buddhasBirthday);
        LocalDate laborDay = LocalDate.of(year, 5, 1);
        if (year >= 2026) baseDates.add(laborDay);
        LocalDate childrensDay = LocalDate.of(year, 5, 5);
        baseDates.add(childrensDay);
        baseDates.add(LocalDate.of(year, 6, 6));

        LocalDate chuseokDay = lunarToSolar(year, 8, 15);
        List<LocalDate> chuseok = List.of(
                chuseokDay.minusDays(1), chuseokDay, chuseokDay.plusDays(1));
        baseDates.addAll(chuseok);
        LocalDate christmas = LocalDate.of(year, 12, 25);
        baseDates.add(christmas);

        Map<LocalDate, Integer> occurrences = new HashMap<>();
        baseDates.forEach(date -> occurrences.merge(date, 1, Integer::sum));
        Set<LocalDate> holidays = new LinkedHashSet<>(baseDates);

        List<LocalDate> weekendOrOverlap = new ArrayList<>(nationalHolidays);
        weekendOrOverlap.add(buddhasBirthday);
        if (year >= 2026) weekendOrOverlap.add(laborDay);
        weekendOrOverlap.add(childrensDay);
        weekendOrOverlap.add(christmas);
        weekendOrOverlap.stream()
                .filter(date -> isWeekend(date) || overlapsAnotherHoliday(date, occurrences))
                .forEach(date -> holidays.add(nextSubstituteAfter(date, holidays)));

        addLunarHolidaySubstitute(seollal, occurrences, holidays);
        addLunarHolidaySubstitute(chuseok, occurrences, holidays);
        return Set.copyOf(holidays);
    }

    private static void addLunarHolidaySubstitute(
            List<LocalDate> holidayPeriod,
            Map<LocalDate, Integer> occurrences,
            Set<LocalDate> holidays) {
        boolean substituteRequired = holidayPeriod.stream().anyMatch(date ->
                date.getDayOfWeek() == DayOfWeek.SUNDAY
                        || overlapsAnotherHoliday(date, occurrences));
        if (substituteRequired) {
            holidays.add(nextSubstituteAfter(holidayPeriod.getLast(), holidays));
        }
    }

    private static boolean overlapsAnotherHoliday(
            LocalDate date, Map<LocalDate, Integer> occurrences) {
        return !isWeekend(date) && occurrences.getOrDefault(date, 0) > 1;
    }

    private static LocalDate nextSubstituteAfter(LocalDate date, Set<LocalDate> holidays) {
        LocalDate candidate = date.plusDays(1);
        while (holidays.contains(candidate) || isWeekend(candidate)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private synchronized LocalDate lunarToSolar(int year, int month, int day) {
        KoreanLunarCalendar calendar = KoreanLunarCalendar.getInstance();
        if (!calendar.setLunarDate(year, month, day, false)) {
            throw new IllegalArgumentException("지원하지 않는 음력 날짜입니다: " + year + "-" + month + "-" + day);
        }
        return LocalDate.parse(calendar.getSolarIsoFormat());
    }
}
