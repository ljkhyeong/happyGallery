package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.app.booking.port.out.ClassReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSlotManagementService implements SlotManagementUseCase {

    private final ClassReaderPort classReaderPort;
    private final SlotReaderPort slotReaderPort;
    private final SlotStorePort slotStorePort;

    public DefaultSlotManagementService(ClassReaderPort classReaderPort,
                                  SlotReaderPort slotReaderPort,
                                  SlotStorePort slotStorePort) {
        this.classReaderPort = classReaderPort;
        this.slotReaderPort = slotReaderPort;
        this.slotStorePort = slotStorePort;
    }

    /** 슬롯을 생성한다. */
    public Slot createSlot(Long classId, LocalDateTime startAt, LocalDateTime endAt) {
        BookingClass bookingClass = classReaderPort.findById(classId)
                .orElseThrow(() -> new NotFoundException("클래스"));

        if (slotReaderPort.existsByBookingClassIdAndStartAt(classId, startAt)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 동일 시간에 슬롯이 존재합니다.");
        }

        return slotStorePort.save(new Slot(bookingClass, startAt, endAt));
    }

    /** 슬롯을 비활성화한다. */
    public Slot deactivateSlot(Long slotId) {
        Slot slot = slotReaderPort.findById(slotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        slot.deactivate();
        return slotStorePort.save(slot);
    }

    /**
     * 예약을 1건 확정한다.
     *
     * <ol>
     *   <li>비관적 락(SELECT FOR UPDATE)으로 슬롯 row를 잠근다.</li>
     *   <li>정원 검사 후 booked_count를 증가한다. 정원 초과 시 {@link com.personal.happygallery.common.error.CapacityExceededException}.</li>
     *   <li>버퍼 윈도우({@code [endAt, endAt + bufferMin)}) 내 활성 슬롯을 비활성화한다.</li>
     * </ol>
     *
     * <p>§5.2 BookingService에서 예약 엔티티 생성 직전에 호출한다.
     */
    public Slot confirmBooking(Long slotId) {
        Slot slot = slotReaderPort.findByIdWithLock(slotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));

        // 락 획득 후 isActive 재확인 — TOCTOU 방어: 락 없이 체크한 후 다른 트랜잭션이 deactivate했을 수 있음
        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }
        slot.incrementBookedCount();
        slotStorePort.save(slot);

        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(slot.getEndAt());
        LocalDateTime windowEnd   = SlotBufferPolicy.bufferWindowEnd(
                slot.getEndAt(), slot.getBookingClass().getBufferMin());

        List<Slot> bufferSlots = slotReaderPort.findActiveInBufferWindow(
                slot.getBookingClass().getId(), windowStart, windowEnd);
        bufferSlots.forEach(s -> {
            s.deactivate();
            slotStorePort.save(s);
        });

        return slot;
    }
}
