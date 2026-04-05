package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.app.booking.port.out.ClassReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
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
                .orElseThrow(NotFoundException.supplier("클래스"));

        if (slotReaderPort.existsByBookingClassIdAndStartAt(classId, startAt)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미 동일 시간에 슬롯이 존재합니다.");
        }

        return slotStorePort.save(new Slot(bookingClass, startAt, endAt));
    }

    /** 슬롯을 비활성화한다. */
    public Slot deactivateSlot(Long slotId) {
        Slot slot = slotReaderPort.findById(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        slot.deactivate();
        return slotStorePort.save(slot);
    }
}
