package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Slot;

/**
 * 슬롯 관리 유스케이스.
 *
 * <p>자동 생성된 예약 회차의 관리자 활성 상태를 변경한다.
 */
public interface SlotManagementUseCase {

    Slot deactivateSlot(Long slotId);

    Slot activateSlot(Long slotId);
}
