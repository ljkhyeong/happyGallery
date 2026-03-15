package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.SlotRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link SlotRepository}(infra) → {@link SlotReaderPort} + {@link SlotStorePort}(app) 브릿지 어댑터.
 */
@Component
class SlotPersistencePortAdapter implements SlotReaderPort, SlotStorePort {

    private final SlotRepository slotRepository;

    SlotPersistencePortAdapter(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Override
    public Optional<Slot> findById(Long id) {
        return slotRepository.findById(id);
    }

    @Override
    public Optional<Slot> findByIdWithLock(Long id) {
        return slotRepository.findByIdWithLock(id);
    }

    @Override
    public boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt) {
        return slotRepository.existsByBookingClassIdAndStartAt(classId, startAt);
    }

    @Override
    public List<Slot> findByBookingClassIdAndIsActiveTrue(Long classId) {
        return slotRepository.findByBookingClassIdAndIsActiveTrue(classId);
    }

    @Override
    public List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId) {
        return slotRepository.findByBookingClassIdOrderByStartAtDesc(classId);
    }

    @Override
    public List<Slot> findAvailableByClassAndDate(Long classId, LocalDateTime dayStart, LocalDateTime dayEnd) {
        return slotRepository.findAvailableByClassAndDate(classId, dayStart, dayEnd);
    }

    @Override
    public List<Slot> findActiveInBufferWindow(Long classId, LocalDateTime windowStart, LocalDateTime windowEnd) {
        return slotRepository.findActiveInBufferWindow(classId, windowStart, windowEnd);
    }

    @Override
    public Slot save(Slot slot) {
        return slotRepository.save(slot);
    }
}
