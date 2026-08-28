package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.out.SlotLockPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSlotManagementService implements SlotManagementUseCase {

    private final SlotLockPort slotLockPort;
    private final SlotStorePort slotStorePort;
    private final BookingVacancyAlertPublisher vacancyAlertPublisher;

    public DefaultSlotManagementService(SlotLockPort slotLockPort,
                                        SlotStorePort slotStorePort,
                                        BookingVacancyAlertPublisher vacancyAlertPublisher) {
        this.slotLockPort = slotLockPort;
        this.slotStorePort = slotStorePort;
        this.vacancyAlertPublisher = vacancyAlertPublisher;
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
        Slot saved = slotStorePort.save(slot);
        vacancyAlertPublisher.notifyWaitingIfAvailable(saved);
        return saved;
    }
}
