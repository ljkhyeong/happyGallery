package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** 비회원 예약 조회 — bookingId + accessToken 두 조건 모두 만족해야 함 */
    Optional<Booking> findByIdAndAccessToken(Long id, String accessToken);

    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.id = :id
              AND b.accessToken = :accessToken
            """)
    Optional<Booking> findDetailByIdAndAccessToken(@Param("id") Long id,
                                                   @Param("accessToken") String accessToken);

    /** 회원 — 자기 예약 조회 (슬롯 시작 시간 내림차순) */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.userId = :userId
            ORDER BY b.slot.startAt DESC
            """)
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.id = :id
              AND b.userId = :userId
            """)
    Optional<Booking> findByIdAndUserIdWithDetails(@Param("id") Long id,
                                                   @Param("userId") Long userId);

    /** guest claim preview용 비회원 예약 조회 (슬롯 시작 시간 내림차순) */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.guest.id = :guestId
            ORDER BY b.slot.startAt DESC
            """)
    List<Booking> findByGuestIdWithDetails(@Param("guestId") Long guestId);

    /** 동일 슬롯 + 동일 회원 중복 예약 확인 */
    boolean existsBySlotIdAndUserId(Long slotId, Long userId);

    /** 동일 슬롯 + 동일 게스트 중복 예약 확인 */
    boolean existsBySlotIdAndGuestId(Long slotId, Long guestId);

    /** 동일 슬롯 + 동일 게스트 중복 예약 확인 — 특정 booking 제외 (변경 시 자기 자신 제외용) */
    boolean existsBySlotIdAndGuestIdAndIdNot(Long slotId, Long guestId, Long excludeBookingId);

    /** 동일 슬롯 + 동일 회원 중복 예약 확인 — 특정 booking 제외 (변경 시 자기 자신 제외용) */
    boolean existsBySlotIdAndUserIdAndIdNot(Long slotId, Long userId, Long excludeBookingId);

    /** 8회권 환불 시 자동취소 대상 — 해당 pass의 미래 BOOKED 예약 */
    @Query("SELECT b FROM Booking b WHERE b.passPurchase.id = :passId AND b.status = :status AND b.slot.startAt > :now")
    List<Booking> findFuturePassBookings(@Param("passId") Long passId,
                                         @Param("status") BookingStatus status,
                                         @Param("now") LocalDateTime now);

    /** D-1 / 당일 리마인드 공용 — JOIN FETCH guest (detached 후 LAZY 로딩 방지) */
    @Query("SELECT b FROM Booking b JOIN FETCH b.guest WHERE b.status = :status AND b.slot.startAt >= :start AND b.slot.startAt < :end")
    List<Booking> findBookingsInRange(@Param("status") BookingStatus status,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /** 관리자 — 날짜 범위 내 예약 전체 조회 (guest, class, slot eager fetch) */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.slot.startAt >= :start AND b.slot.startAt < :end
            ORDER BY b.slot.startAt ASC
            """)
    List<Booking> findAllInRange(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}
