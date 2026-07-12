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

    /** 슬롯을 잠근 뒤 활성 상태를 재확인하고 정원을 확보한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void reserveCapacity(Long slotId) {
        Slot slot = slotReaderPort.findByIdWithLock(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));

        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }
        slot.incrementBookedCount();
        slotStorePort.save(slot);

        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(slot.getEndAt());
        LocalDateTime windowEnd = SlotBufferPolicy.bufferWindowEnd(
                slot.getEndAt(), slot.getBookingClass().getBufferMin());

        List<Slot> bufferSlots = slotReaderPort.findActiveInBufferWindow(
                slot.getBookingClass().getId(), windowStart, windowEnd);
        bufferSlots.forEach(bufferSlot -> {
            bufferSlot.deactivate();
            slotStorePort.save(bufferSlot);
        });
    }

    /** 슬롯을 잠근 뒤 사용 중인 정원 하나를 반납한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    Slot releaseCapacity(Long slotId) {
        Slot slot = slotReaderPort.findByIdWithLock(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        slot.decrementBookedCount();
        slotStorePort.save(slot);
        return slot;
    }
}
