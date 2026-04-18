package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class SlotBookingSupport {

    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;

    SlotBookingSupport(SlotReaderPort slotReaderPort,
                       SlotStorePort slotStorePort) {
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmBooking(Long slotId) {
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

    @Transactional(propagation = Propagation.MANDATORY)
    public Slot releaseSlotCapacity(Long slotId) {
        Slot slot = slotReaderPort.findByIdWithLock(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        slot.decrementBookedCount();
        slotStorePort.save(slot);
        return slot;
    }
}
