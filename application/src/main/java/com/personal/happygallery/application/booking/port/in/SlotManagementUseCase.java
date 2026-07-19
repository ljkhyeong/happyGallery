package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;

/**
 * 슬롯 관리 유스케이스.
 *
 * <p>슬롯 생성·비활성화를 담당한다. 종료 시각은 클래스 소요 시간으로 계산한다.
 */
public interface SlotManagementUseCase {

    Slot createSlot(Long classId, LocalDateTime startAt);

    Slot deactivateSlot(Long slotId);

    Slot activateSlot(Long slotId);
}
