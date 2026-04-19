package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;

/**
 * 슬롯 관리 유스케이스.
 *
 * <p>슬롯 생성·비활성화를 담당한다.
 */
public interface SlotManagementUseCase {

    Slot createSlot(Long classId, LocalDateTime startAt, LocalDateTime endAt);

    Slot deactivateSlot(Long slotId);
}
