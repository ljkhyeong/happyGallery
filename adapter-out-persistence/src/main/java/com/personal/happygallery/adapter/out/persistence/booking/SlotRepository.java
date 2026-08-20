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
                s.adminActive, s.bufferBlockCount, s.bookedCount,
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
                s.adminActive, s.bufferBlockCount, s.bookedCount,
                s.bookingClass.bufferMin, s.bookingClass.price,
                s.bookingClass.category, s.bookingClass.passEligible
            )
            FROM Slot s
            WHERE s.id IN :ids
            """)
    List<SlotSchedulingSnapshot> findSchedulingSnapshotsByIdIn(@Param("ids") Iterable<Long> ids);

    @Override
    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Slot s
            WHERE s.bookingClass.id = :classId
              AND s.id <> :sourceSlotId
              AND s.startAt >= :windowStart
              AND s.startAt < :windowEnd
              AND s.bookedCount > 0
            """)
    boolean existsBookedInBufferWindow(
            @Param("classId") Long classId,
            @Param("sourceSlotId") Long sourceSlotId,
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd);

    /** 중복 슬롯 검사 — (class_id, start_at) UNIQUE 제약 반영 */
    @Override boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    /** 관리자 슬롯 전체 조회 — 활성/비활성 포함, 시작 시각 내림차순 */
    @Override List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    /** 공개 슬롯 탐색 — 향후 기간 내 예약 가능한 슬롯을 시작 시각 순으로 조회한다. */
    @Override
    @Query("SELECT s FROM Slot s " +
           "WHERE s.bookingClass.id = :classId " +
           "AND s.bookingClass.status = com.personal.happygallery.domain.booking.BookingClassStatus.ACTIVE " +
           "AND s.startAt >= :rangeStart AND s.startAt < :rangeEnd " +
           "AND s.startAt > :now " +
           "AND s.adminActive = true " +
           "AND s.bufferBlockCount = 0 " +
           "AND s.bookedCount < s.capacity " +
           "ORDER BY s.startAt")
    List<Slot> findAvailableByClassAndRange(@Param("classId") Long classId,
                                            @Param("rangeStart") LocalDateTime rangeStart,
                                            @Param("rangeEnd") LocalDateTime rangeEnd,
                                            @Param("now") LocalDateTime now);

}
