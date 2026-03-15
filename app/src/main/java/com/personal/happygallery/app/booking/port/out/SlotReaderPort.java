package com.personal.happygallery.app.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotReaderPort {

    Optional<Slot> findById(Long id);

    /** 비관적 쓰기 락 — 정원 강제용 */
    Optional<Slot> findByIdWithLock(Long id);

    boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    List<Slot> findByBookingClassIdAndIsActiveTrue(Long classId);

    List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    List<Slot> findAvailableByClassAndDate(Long classId, LocalDateTime dayStart, LocalDateTime dayEnd);

    List<Slot> findActiveInBufferWindow(Long classId, LocalDateTime windowStart, LocalDateTime windowEnd);
}
