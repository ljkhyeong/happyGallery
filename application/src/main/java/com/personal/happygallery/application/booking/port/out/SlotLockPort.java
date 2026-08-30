package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
import java.util.List;

public interface SlotLockPort {

    /** 관련 슬롯을 PK 오름차순으로 잠그고 DB의 최신 상태로 반환한다. */
    List<Slot> lockAllById(List<Long> ids);

    /** 원본 슬롯과 수업·정리 시간이 충돌하는 슬롯을 현재 읽기로 잠근다. */
    List<Slot> lockScope(Long classId,
                         Long sourceSlotId,
                         LocalDateTime sourceStartAt,
                         LocalDateTime sourceEndWithBuffer);
}
