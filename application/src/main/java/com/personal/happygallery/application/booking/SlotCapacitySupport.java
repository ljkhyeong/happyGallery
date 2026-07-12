package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 슬롯 활성 확인과 비관적 잠금 기반 정원 확보·반납을 담당한다. */
@Component
class SlotCapacitySupport {

    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;

    SlotCapacitySupport(SlotReaderPort slotReaderPort,
                        SlotStorePort slotStorePort) {
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
    }

    /** 잠금 전에 존재 여부와 활성 상태를 빠르게 확인한다. */
    Slot loadActiveSlot(Long slotId) {
        Slot slot = slotReaderPort.findById(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }
        return slot;
    }

    /** 슬롯을 잠근 뒤 활성 상태를 재확인하고 정원을 확보한다. 첫 예약이면 뒤쪽 버퍼를 차단한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void reserveCapacity(Long slotId) {
        Slot slot = slotReaderPort.findByIdWithLock(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));

        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }
        boolean firstBooking = slot.getBookedCount() == 0;
        slot.incrementBookedCount();
        slotStorePort.save(slot);

        if (firstBooking) {
            blockBufferSlots(slot);
        }
    }

    /** 슬롯을 잠근 뒤 정원을 반납한다. 마지막 예약이면 뒤쪽 버퍼 차단을 해제한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Slot releaseCapacity(Long slotId) {
        Slot slot = slotReaderPort.findByIdWithLock(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        slot.decrementBookedCount();
        slotStorePort.save(slot);
        if (slot.getBookedCount() == 0) {
            releaseBufferSlots(slot);
        }
        return slot;
    }

    private void blockBufferSlots(Slot sourceSlot) {
        for (Slot bufferSlot : findBufferSlotsWithLock(sourceSlot)) {
            bufferSlot.incrementBufferBlockCount();
            slotStorePort.save(bufferSlot);
        }
    }

    private void releaseBufferSlots(Slot sourceSlot) {
        for (Slot bufferSlot : findBufferSlotsWithLock(sourceSlot)) {
            bufferSlot.decrementBufferBlockCount();
            slotStorePort.save(bufferSlot);
        }
    }

    private List<Slot> findBufferSlotsWithLock(Slot sourceSlot) {
        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(sourceSlot.getEndAt());
        LocalDateTime windowEnd = SlotBufferPolicy.bufferWindowEnd(
                sourceSlot.getEndAt(), sourceSlot.getBookingClass().getBufferMin());
        return slotReaderPort.findInBufferWindowWithLock(
                sourceSlot.getBookingClass().getId(), windowStart, windowEnd);
    }
}
