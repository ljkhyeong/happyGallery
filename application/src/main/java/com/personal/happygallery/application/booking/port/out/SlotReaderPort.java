package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotReaderPort {

    Optional<Slot> findById(Long id);

    List<Slot> findAllById(Iterable<Long> ids);

    Optional<SlotSchedulingSnapshot> findSchedulingSnapshotById(Long id);

    List<SlotSchedulingSnapshot> findSchedulingSnapshotsByIdIn(Iterable<Long> ids);

    /** 수업 시간과 뒤쪽 정리 버퍼가 겹치는 예약된 다른 슬롯이 있는지 확인한다. */
    long countBookedConflicts(
            Long classId,
            Long sourceSlotId,
            LocalDateTime sourceStartAt,
            LocalDateTime sourceEndWithBuffer);

    List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    List<Slot> findByBookingClassIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
            Long classId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

}
