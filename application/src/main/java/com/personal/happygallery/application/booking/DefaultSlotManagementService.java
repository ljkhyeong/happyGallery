package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotLockPort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSlotManagementService implements SlotManagementUseCase {

    private final ClassReaderPort classReaderPort;
    private final SlotLockPort slotLockPort;
    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;
    private final Clock clock;

    public DefaultSlotManagementService(ClassReaderPort classReaderPort,
                                        SlotLockPort slotLockPort,
                                        SlotReaderPort slotReaderPort,
                                        SlotStorePort slotStorePort,
                                        Clock clock) {
        this.classReaderPort = classReaderPort;
        this.slotLockPort = slotLockPort;
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
        this.clock = clock;
    }

    /** 슬롯을 생성한다. */
    @Override
    public Slot createSlot(Long classId, LocalDateTime startAt) {
        BookingClass bookingClass = classReaderPort.findByIdForUpdate(classId)
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.requireActive();
        requireFuture(startAt);

        if (slotReaderPort.existsByBookingClassIdAndStartAt(classId, startAt)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 동일 시간에 슬롯이 존재합니다.");
        }

        Slot slot = newSlot(
                bookingClass,
                startAt,
                slotReaderPort.findByBookingClassIdOrderByStartAtDesc(classId));
        return slotStorePort.save(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public BulkSlotResult previewBulkSlots(BulkSlotCommand command) {
        BookingClass bookingClass = classReaderPort.findById(command.classId())
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.requireActive();
        return evaluateBulkSlots(command, bookingClass, false);
    }

    @Override
    public BulkSlotResult createBulkSlots(BulkSlotCommand command) {
        BookingClass bookingClass = classReaderPort.findByIdForUpdate(command.classId())
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.requireActive();
        return evaluateBulkSlots(command, bookingClass, true);
    }

    private BulkSlotResult evaluateBulkSlots(BulkSlotCommand command,
                                             BookingClass bookingClass,
                                             boolean create) {
        List<LocalDateTime> candidates = generateCandidates(command);
        List<Slot> existingSlots = slotReaderPort
                .findByBookingClassIdOrderByStartAtDesc(command.classId());
        Set<LocalDateTime> existingStarts = new HashSet<>();
        existingSlots.forEach(slot -> existingStarts.add(slot.getStartAt()));
        LocalDateTime now = LocalDateTime.now(clock);

        List<BulkSlotItem> items = candidates.stream()
                .map(startAt -> evaluateCandidate(
                        bookingClass, existingSlots, existingStarts, startAt, now, create))
                .toList();
        return new BulkSlotResult(items);
    }

    private BulkSlotItem evaluateCandidate(BookingClass bookingClass,
                                           List<Slot> existingSlots,
                                           Set<LocalDateTime> existingStarts,
                                           LocalDateTime startAt,
                                           LocalDateTime now,
                                           boolean create) {
        LocalDateTime endAt = startAt.plusMinutes(bookingClass.getDurationMin());
        boolean bufferBlocked = existingSlots.stream()
                .filter(Slot::hasBookings)
                .anyMatch(existing -> SlotBufferPolicy.contains(
                        existing.getEndAt(), bookingClass.getBufferMin(), startAt));

        if (!startAt.isAfter(now)) {
            return new BulkSlotItem(
                    null, startAt, endAt, BulkSlotStatus.SKIPPED_PAST, bufferBlocked);
        }
        if (existingStarts.contains(startAt)) {
            return new BulkSlotItem(
                    null, startAt, endAt, BulkSlotStatus.SKIPPED_DUPLICATE, bufferBlocked);
        }
        if (!create) {
            return new BulkSlotItem(
                    null, startAt, endAt, BulkSlotStatus.CREATABLE, bufferBlocked);
        }

        Slot slot = slotStorePort.save(newSlot(bookingClass, startAt, existingSlots));
        return new BulkSlotItem(
                slot.getId(), startAt, slot.getEndAt(), BulkSlotStatus.CREATED, slot.isBufferBlocked());
    }

    private List<LocalDateTime> generateCandidates(BulkSlotCommand command) {
        validateBulkCommand(command);
        return command.dateFrom()
                .datesUntil(command.dateTo().plusDays(1))
                .filter(date -> command.weekdays().contains(date.getDayOfWeek()))
                .flatMap(date -> command.startTimes().stream().sorted().map(date::atTime))
                .toList();
    }

    private void validateBulkCommand(BulkSlotCommand command) {
        if (command.dateFrom() == null || command.dateTo() == null
                || command.dateFrom().isAfter(command.dateTo())) {
            throw invalidBulkRequest("시작일은 종료일보다 늦을 수 없습니다.");
        }
        long dateRangeDays = ChronoUnit.DAYS.between(command.dateFrom(), command.dateTo()) + 1;
        if (dateRangeDays > MAX_BULK_DATE_RANGE_DAYS) {
            throw invalidBulkRequest("슬롯 생성 기간은 최대 93일입니다.");
        }
        if (command.weekdays().isEmpty() || command.startTimes().isEmpty()) {
            throw invalidBulkRequest("요일과 시작 시각을 한 개 이상 선택해야 합니다.");
        }
        long candidateCount = command.dateFrom()
                .datesUntil(command.dateTo().plusDays(1))
                .filter(date -> command.weekdays().contains(date.getDayOfWeek()))
                .count() * command.startTimes().size();
        if (candidateCount > MAX_BULK_CANDIDATES) {
            throw invalidBulkRequest("한 번에 생성할 수 있는 슬롯은 최대 500개입니다.");
        }
    }

    private Slot newSlot(BookingClass bookingClass,
                         LocalDateTime startAt,
                         List<Slot> existingSlots) {
        Slot slot = new Slot(bookingClass, startAt);
        existingSlots.stream()
                .filter(Slot::hasBookings)
                .filter(existing -> SlotBufferPolicy.contains(
                        existing.getEndAt(), bookingClass.getBufferMin(), startAt))
                .forEach(existing -> slot.incrementBufferBlockCount());
        return slot;
    }

    private void requireFuture(LocalDateTime startAt) {
        if (startAt == null || !startAt.isAfter(LocalDateTime.now(clock))) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "미래 시각의 슬롯만 생성할 수 있습니다.");
        }
    }

    private static HappyGalleryException invalidBulkRequest(String message) {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, message);
    }

    /** 슬롯을 비활성화한다. */
    @Override
    public Slot deactivateSlot(Long slotId) {
        Slot slot = slotLockPort.lockAllById(List.of(slotId)).stream()
                .findFirst()
                .orElseThrow(NotFoundException.supplier("슬롯"));
        slot.deactivate();
        return slotStorePort.save(slot);
    }

    /** 슬롯의 관리자 활성 상태를 복구한다. 버퍼 차단 수는 변경하지 않는다. */
    @Override
    public Slot activateSlot(Long slotId) {
        Slot slot = slotLockPort.lockAllById(List.of(slotId)).stream()
                .findFirst()
                .orElseThrow(NotFoundException.supplier("슬롯"));
        slot.activate();
        return slotStorePort.save(slot);
    }
}
