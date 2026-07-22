package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingClassLockPort;
import com.personal.happygallery.application.booking.port.out.SlotLockPort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 슬롯 활성 확인과 비관적 잠금 기반 정원 확보·반납을 담당한다. */
@Component
class SlotCapacitySupport {

    private final SlotReaderPort slotReaderPort;
    private final ClassReaderPort classReaderPort;
    private final BookingClassLockPort bookingClassLockPort;
    private final SlotLockPort slotLockPort;
    private final SlotStorePort slotStorePort;
    private final Clock clock;

    SlotCapacitySupport(SlotReaderPort slotReaderPort,
                        ClassReaderPort classReaderPort,
                        BookingClassLockPort bookingClassLockPort,
                        SlotLockPort slotLockPort,
                        SlotStorePort slotStorePort,
                        Clock clock) {
        this.slotReaderPort = slotReaderPort;
        this.classReaderPort = classReaderPort;
        this.bookingClassLockPort = bookingClassLockPort;
        this.slotLockPort = slotLockPort;
        this.slotStorePort = slotStorePort;
        this.clock = clock;
    }

    /** 잠금 전에 존재 여부와 활성 상태를 빠르게 확인한다. */
    Slot requireAvailableSlot(Long slotId) {
        Slot slot = slotReaderPort.findById(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        if (!slot.isReservableAt(LocalDateTime.now(clock))) {
            throw new SlotNotAvailableException();
        }
        return slot;
    }

    /** 다중 슬롯 작업 전에 관련 클래스 행을 PK 순서로 모두 잠근다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void lockClassesForSlots(List<Long> slotIds) {
        Set<Long> expectedSlotIds = Set.copyOf(slotIds);
        if (expectedSlotIds.isEmpty()) {
            return;
        }

        List<Slot> slots = slotReaderPort.findAllById(expectedSlotIds);
        if (slots.size() != expectedSlotIds.size()) {
            throw new NotFoundException("슬롯");
        }
        List<Long> classIds = slots.stream()
                .map(slot -> slot.getBookingClass().getId())
                .distinct()
                .sorted()
                .toList();
        if (classReaderPort.findAllByIdForUpdate(classIds).size() != classIds.size()) {
            throw new NotFoundException("클래스");
        }
    }

    /** 슬롯을 잠근 뒤 활성 상태를 재확인하고 정원을 확보한다. 첫 예약이면 뒤쪽 버퍼를 차단한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Slot reserveCapacity(Long slotId) {
        LockedSlotScope locked = lockCapacityScope(slotId);
        Slot slot = locked.source();

        if (!slot.isReservableAt(LocalDateTime.now(clock))) {
            throw new SlotNotAvailableException();
        }

        List<Slot> bufferSlots = locked.bufferSlots();
        if (bufferSlots.stream().anyMatch(Slot::hasBookings)) {
            throw new SlotNotAvailableException();
        }

        boolean firstBooking = !slot.hasBookings();
        slot.incrementBookedCount();
        slotStorePort.save(slot);

        if (firstBooking) {
            blockBufferSlots(bufferSlots);
        }
        return slot;
    }

    /** 슬롯을 잠근 뒤 정원을 반납한다. 마지막 예약이면 뒤쪽 버퍼 차단을 해제한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Slot releaseCapacity(Long slotId) {
        return releaseCapacity(lockCapacityScope(slotId));
    }

    /** 신규 접수가 중단된 최신 슬롯 상태를 잠금 아래 확인하고 수업 취소 동안 잠금을 유지한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    LockedSlotScope lockInactiveSessionSlot(Long slotId) {
        LockedSlotScope locked = lockCapacityScope(slotId);
        if (locked.source().isAdminActive()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "신규 접수를 중단한 비활성 슬롯만 수업을 취소할 수 있습니다.");
        }
        return locked;
    }

    /** 이미 잠근 수업 슬롯에서 예약 한 건의 정원을 반납한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Slot releaseCapacity(LockedSlotScope locked) {
        Slot slot = locked.source();
        slot.decrementBookedCount();
        slotStorePort.save(slot);
        if (!slot.hasBookings()) {
            releaseBufferSlots(locked.bufferSlots());
        }
        return slot;
    }

    private void blockBufferSlots(List<Slot> bufferSlots) {
        for (Slot bufferSlot : bufferSlots) {
            bufferSlot.incrementBufferBlockCount();
            slotStorePort.save(bufferSlot);
        }
    }

    private void releaseBufferSlots(List<Slot> bufferSlots) {
        for (Slot bufferSlot : bufferSlots) {
            bufferSlot.decrementBufferBlockCount();
            slotStorePort.save(bufferSlot);
        }
    }

    /** 클래스와 슬롯 범위를 순서대로 잠그고 후속 정원 변경까지 유지할 범위를 반환한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    LockedSlotScope lockCapacityScope(Long slotId) {
        Slot sourceSnapshot = slotReaderPort.findById(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        Long classId = sourceSnapshot.getBookingClass().getId();
        BookingClass lockedClass = bookingClassLockPort.lockFresh(classId)
                .orElseThrow(NotFoundException.supplier("클래스"));
        LocalDateTime windowEnd = SlotBufferPolicy.bufferWindowEnd(
                sourceSnapshot.getEndAt(), lockedClass.getBufferMin());
        List<Slot> lockedSlots = slotLockPort.lockScope(
                classId, slotId, sourceSnapshot.getEndAt(), windowEnd);
        Slot source = lockedSlots.stream()
                .filter(slot -> slot.getId().equals(slotId))
                .findFirst()
                .orElseThrow(NotFoundException.supplier("슬롯"));
        List<Slot> bufferSlots = lockedSlots.stream()
                .filter(slot -> !slot.getId().equals(slotId))
                .toList();
        return new LockedSlotScope(source, bufferSlots);
    }

    record LockedSlotScope(Slot source, List<Slot> bufferSlots) {}
}
