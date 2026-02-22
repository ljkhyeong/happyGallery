package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Slot;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    /** 중복 슬롯 검사 — (class_id, start_at) UNIQUE 제약 반영 */
    boolean existsByBookingClassIdAndStartAt(Long classId, LocalDateTime startAt);

    /** 관리자 슬롯 목록 조회 — 활성 슬롯만 */
    List<Slot> findByBookingClassIdAndIsActiveTrue(Long classId);

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
