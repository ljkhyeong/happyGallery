package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotReaderPort {

    Optional<Slot> findById(Long id);

    boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    List<Slot> findAvailableByClassAndDate(Long classId, LocalDateTime dayStart,
                                           LocalDateTime dayEnd, LocalDateTime now);

}
