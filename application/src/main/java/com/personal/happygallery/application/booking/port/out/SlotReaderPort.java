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

    /** 뒤쪽 버퍼 범위 [windowStart, windowEnd)에 예약된 다른 슬롯이 있는지 확인한다. */
    boolean existsBookedInBufferWindow(
            Long classId,
            Long sourceSlotId,
            LocalDateTime windowStart,
            LocalDateTime windowEnd);

    boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    List<Slot> findAvailableByClassAndRange(Long classId, LocalDateTime rangeStart,
                                            LocalDateTime rangeEnd, LocalDateTime now);

}
