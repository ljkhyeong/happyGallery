package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotSchedulingSnapshot;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long>, SlotReaderPort, SlotStorePort {

    @Override
    <S extends Slot> S save(S slot);

    @Override
    <S extends Slot> List<S> saveAll(Iterable<S> slots);

    @Override Optional<Slot> findById(Long id);
    @Override List<Slot> findAllById(Iterable<Long> ids);

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.booking.port.out.SlotSchedulingSnapshot(
                s.id, s.bookingClass.id, s.startAt, s.endAt, s.bookingClass.status,
                s.adminActive, s.calendarActive, s.bufferBlockCount, s.capacity, s.bookedCount,
                s.bookingClass.bufferMin, s.bookingClass.price,
                s.bookingClass.category, s.bookingClass.passEligible
            )
            FROM Slot s
            WHERE s.id = :id
            """)
    Optional<SlotSchedulingSnapshot> findSchedulingSnapshotById(@Param("id") Long id);

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.booking.port.out.SlotSchedulingSnapshot(
                s.id, s.bookingClass.id, s.startAt, s.endAt, s.bookingClass.status,
                s.adminActive, s.calendarActive, s.bufferBlockCount, s.capacity, s.bookedCount,
                s.bookingClass.bufferMin, s.bookingClass.price,
                s.bookingClass.category, s.bookingClass.passEligible
            )
            FROM Slot s
            WHERE s.id IN :ids
            """)
    List<SlotSchedulingSnapshot> findSchedulingSnapshotsByIdIn(@Param("ids") Iterable<Long> ids);

    @Override
    @Query(value = """
            SELECT COUNT(*)
            FROM slots candidate
            JOIN classes booking_class ON booking_class.id = candidate.class_id
            WHERE candidate.class_id = :classId
              AND candidate.id <> :sourceSlotId
              AND candidate.booked_count > 0
              AND candidate.start_at < :sourceEndWithBuffer
              AND TIMESTAMPADD(MINUTE, booking_class.buffer_min, candidate.end_at) > :sourceStartAt
            """, nativeQuery = true)
    long countBookedConflicts(
            @Param("classId") Long classId,
            @Param("sourceSlotId") Long sourceSlotId,
            @Param("sourceStartAt") LocalDateTime sourceStartAt,
            @Param("sourceEndWithBuffer") LocalDateTime sourceEndWithBuffer);

    /** 관리자 슬롯 전체 조회 — 활성/비활성 포함, 시작 시각 내림차순 */
    @Override List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    @Override
    List<Slot> findByBookingClassIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
            Long classId, LocalDateTime rangeStart, LocalDateTime rangeEnd);

}
