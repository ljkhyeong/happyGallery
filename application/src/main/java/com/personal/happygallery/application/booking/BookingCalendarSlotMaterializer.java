package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 캘린더 규칙을 실제 예약 잠금 단위인 슬롯 행으로 반영한다. */
@Component
class BookingCalendarSlotMaterializer {

    private final BookingCalendarPolicy calendarPolicy;
    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;
    private final Clock clock;

    BookingCalendarSlotMaterializer(BookingCalendarPolicy calendarPolicy,
                                    SlotReaderPort slotReaderPort,
                                    SlotStorePort slotStorePort,
                                    Clock clock) {
        this.calendarPolicy = calendarPolicy;
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
        this.clock = clock;
    }

    List<Slot> materialize(BookingClass bookingClass,
                           LocalDateTime rangeStart,
                           LocalDateTime rangeEnd) {
        return materialize(bookingClass, rangeStart, rangeEnd, false);
    }

    List<Slot> materializeIncludingFull(BookingClass bookingClass,
                                        LocalDateTime rangeStart,
                                        LocalDateTime rangeEnd) {
        return materialize(bookingClass, rangeStart, rangeEnd, true);
    }

    private List<Slot> materialize(BookingClass bookingClass,
                                   LocalDateTime rangeStart,
                                   LocalDateTime rangeEnd,
                                   boolean includeFull) {
        LocalDate dateFrom = rangeStart.toLocalDate();
        LocalDate dateTo = rangeEnd.minusNanos(1).toLocalDate();
        BookingCalendarPolicy.CalendarRules rules = calendarPolicy.rules(dateFrom, dateTo);
        // 전날 슬롯의 뒤쪽 버퍼도 판단해야 하므로 하루 앞부터 한 번에 조회한다.
        List<Slot> nearbySlots = slotReaderPort
                .findByBookingClassIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
                        bookingClass.getId(), rangeStart.minusDays(1), rangeEnd);
        List<Slot> existing = nearbySlots.stream()
                .filter(slot -> !slot.getStartAt().isBefore(rangeStart))
                .toList();
        List<Slot> changed = refresh(existing, rules);

        Map<LocalDateTime, Slot> slotsByStart = new HashMap<>();
        existing.forEach(slot -> slotsByStart.put(slot.getStartAt(), slot));
        Set<LocalDateTime> starts = new HashSet<>(calendarPolicy.availableStarts(
                bookingClass, dateFrom, dateTo, LocalDateTime.now(clock), rules));

        for (LocalDateTime startAt : starts) {
            if (slotsByStart.containsKey(startAt)) continue;
            Slot slot = new Slot(bookingClass, startAt);
            for (Slot bookedSlot : nearbySlots) {
                if (bookedSlot.hasBookings() && SlotBufferPolicy.conflicts(
                        slot.getStartAt(), slot.getEndAt(),
                        bookedSlot.getStartAt(), bookedSlot.getEndAt(),
                        bookingClass.getBufferMin())) {
                    slot.incrementBufferBlockCount();
                }
            }
            slotsByStart.put(startAt, slot);
            changed.add(slot);
        }
        if (!changed.isEmpty()) {
            slotStorePort.saveAll(changed);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return slotsByStart.values().stream()
                .filter(slot -> slot.isReservableAt(now))
                .filter(slot -> includeFull || slot.getBookedCount() < slot.getCapacity())
                .sorted(Comparator.comparing(Slot::getStartAt))
                .toList();
    }

    void refreshExisting(BookingClass bookingClass,
                         LocalDateTime rangeStart,
                         LocalDateTime rangeEnd) {
        BookingCalendarPolicy.CalendarRules rules = calendarPolicy.rules(
                rangeStart.toLocalDate(), rangeEnd.minusNanos(1).toLocalDate());
        List<Slot> existing = slotReaderPort
                .findByBookingClassIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
                        bookingClass.getId(), rangeStart, rangeEnd);
        List<Slot> changed = refresh(existing, rules);
        if (!changed.isEmpty()) {
            slotStorePort.saveAll(changed);
        }
    }

    private List<Slot> refresh(List<Slot> slots,
                               BookingCalendarPolicy.CalendarRules rules) {
        List<Slot> changed = new ArrayList<>();
        for (Slot slot : slots) {
            boolean available = calendarPolicy.isAvailable(
                    slot.getStartAt(), slot.getEndAt(), rules);
            if (slot.isCalendarActive() != available) {
                slot.applyCalendarAvailability(available);
                changed.add(slot);
            }
        }
        return changed;
    }
}
