package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long>, SlotReaderPort, SlotStorePort {

    @Override Optional<Slot> findById(Long id);
    @Override List<Slot> findAllById(Iterable<Long> ids);
    @Override Slot save(Slot slot);

    /** 중복 슬롯 검사 — (class_id, start_at) UNIQUE 제약 반영 */
    @Override boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    /** 관리자 슬롯 전체 조회 — 활성/비활성 포함, 시작 시각 내림차순 */
    @Override List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    /** 공개 슬롯 조회 — classId + 날짜 기준, 활성 & 잔여 정원 있는 슬롯만 */
    @Override
    @Query("SELECT s FROM Slot s " +
           "WHERE s.bookingClass.id = :classId " +
           "AND s.bookingClass.status = com.personal.happygallery.domain.booking.BookingClassStatus.ACTIVE " +
           "AND s.startAt >= :dayStart AND s.startAt < :dayEnd " +
           "AND s.startAt > :now " +
           "AND s.adminActive = true " +
           "AND s.bufferBlockCount = 0 " +
           "AND s.bookedCount < s.capacity " +
           "ORDER BY s.startAt")
    List<Slot> findAvailableByClassAndDate(@Param("classId") Long classId,
                                           @Param("dayStart") LocalDateTime dayStart,
                                           @Param("dayEnd") LocalDateTime dayEnd,
                                           @Param("now") LocalDateTime now);

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
