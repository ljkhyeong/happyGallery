package com.personal.happygallery.app.booking;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.common.error.SlotNotAvailableException;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SlotManagementService {

    private final ClassRepository classRepository;
    private final SlotRepository slotRepository;

    public SlotManagementService(ClassRepository classRepository, SlotRepository slotRepository) {
        this.classRepository = classRepository;
        this.slotRepository = slotRepository;
    }

    /** 슬롯을 생성한다. */
    public Slot createSlot(Long classId, LocalDateTime startAt, LocalDateTime endAt) {
        BookingClass bookingClass = classRepository.findById(classId)
                .orElseThrow(() -> new NotFoundException("클래스"));

        if (slotRepository.existsByBookingClassIdAndStartAt(classId, startAt)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 동일 시간에 슬롯이 존재합니다.");
        }

        return slotRepository.save(new Slot(bookingClass, startAt, endAt));
    }

    /** 슬롯을 비활성화한다. */
    public Slot deactivateSlot(Long slotId) {
        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));
        slot.deactivate();
        return slotRepository.save(slot);
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
        Slot slot = slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new NotFoundException("슬롯"));

        // 락 획득 후 isActive 재확인 — TOCTOU 방어: 락 없이 체크한 후 다른 트랜잭션이 deactivate했을 수 있음
        if (!slot.isActive()) {
            throw new SlotNotAvailableException();
        }
        slot.incrementBookedCount();
        slotRepository.save(slot);

        LocalDateTime windowStart = SlotBufferPolicy.bufferWindowStart(slot.getEndAt());
        LocalDateTime windowEnd   = SlotBufferPolicy.bufferWindowEnd(
                slot.getEndAt(), slot.getBookingClass().getBufferMin());

        List<Slot> bufferSlots = slotRepository.findActiveInBufferWindow(
                slot.getBookingClass().getId(), windowStart, windowEnd);
        bufferSlots.forEach(s -> {
            s.deactivate();
            slotRepository.save(s);
        });

        return slot;
    }
}
