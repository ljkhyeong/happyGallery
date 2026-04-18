package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.Slot;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long>, SlotReaderPort, SlotStorePort {

    @Override Optional<Slot> findById(Long id);
    @Override Slot save(Slot slot);

    /** 중복 슬롯 검사 — (class_id, start_at) UNIQUE 제약 반영 */
    boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    /** 관리자 슬롯 목록 조회 — 활성 슬롯만 */
    List<Slot> findByBookingClassIdAndIsActiveTrue(Long classId);

    /** 관리자 슬롯 전체 조회 — 활성/비활성 포함, 시작 시각 내림차순 */
    List<Slot> findByBookingClassIdOrderByStartAtDesc(Long classId);

    /** 공개 슬롯 조회 — classId + 날짜 기준, 활성 & 잔여 정원 있는 슬롯만 */
    @Query("SELECT s FROM Slot s " +
           "WHERE s.bookingClass.id = :classId " +
           "AND s.startAt >= :dayStart AND s.startAt < :dayEnd " +
           "AND s.isActive = true " +
           "AND s.bookedCount < s.capacity " +
           "ORDER BY s.startAt")
    List<Slot> findAvailableByClassAndDate(@Param("classId") Long classId,
                                           @Param("dayStart") LocalDateTime dayStart,
                                           @Param("dayEnd") LocalDateTime dayEnd);

    /** 비관적 쓰기 락 — 정원 강제용. 반드시 트랜잭션 안에서 호출해야 한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :id")
    Optional<Slot> findByIdWithLock(@Param("id") Long id);

    /**
     * 버퍼 범위 내 활성 슬롯 조회.
     * 범위: {@code start_at in [windowStart, windowEnd)} — 시작 포함, 끝 미포함.
     */
    @Query("SELECT s FROM Slot s " +
           "WHERE s.bookingClass.id = :classId " +
           "AND s.startAt >= :windowStart AND s.startAt < :windowEnd " +
           "AND s.isActive = true")
    List<Slot> findActiveInBufferWindow(@Param("classId") Long classId,
                                        @Param("windowStart") LocalDateTime windowStart,
                                        @Param("windowEnd") LocalDateTime windowEnd);
}
