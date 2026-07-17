package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, BookingReaderPort, BookingStorePort {

    @Override Optional<Booking> findById(Long id);
    @Override Booking save(Booking booking);

    /** 비회원 예약 조회 — bookingId + accessToken 두 조건 모두 만족해야 함 */
    Optional<Booking> findByIdAndAccessToken(Long id, String accessToken);

    @Override
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
    @Override
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.userId = :userId
            ORDER BY b.slot.startAt DESC
            """)
    List<Booking> findByUserIdWithDetails(@Param("userId") Long userId);

    @Override
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
    @Override
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.guest.id = :guestId
            ORDER BY b.slot.startAt DESC
            """)
    List<Booking> findByGuestIdWithDetails(@Param("guestId") Long guestId);

    /** 동일 슬롯에 같은 회원의 활성 예약이 있는지 확인한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.userId = :userId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedBySlotIdAndUserId(@Param("slotId") Long slotId,
                                          @Param("userId") Long userId);

    /** 동일 슬롯에 같은 게스트의 활성 예약이 있는지 확인한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.guest.id = :guestId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedBySlotIdAndGuestId(@Param("slotId") Long slotId,
                                           @Param("guestId") Long guestId);

    /** 예약 변경 시 자기 자신을 제외하고 같은 게스트의 활성 예약이 있는지 확인한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.guest.id = :guestId
              AND b.id <> :excludeBookingId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedBySlotIdAndGuestIdAndIdNot(@Param("slotId") Long slotId,
                                                   @Param("guestId") Long guestId,
                                                   @Param("excludeBookingId") Long excludeBookingId);

    /** 예약 변경 시 자기 자신을 제외하고 같은 회원의 활성 예약이 있는지 확인한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.userId = :userId
              AND b.id <> :excludeBookingId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedBySlotIdAndUserIdAndIdNot(@Param("slotId") Long slotId,
                                                  @Param("userId") Long userId,
                                                  @Param("excludeBookingId") Long excludeBookingId);

    /** 이력 가져오기 시 회원에게 선택 슬롯의 활성 예약이 이미 있는지 한 번에 확인한다. */
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.userId = :userId
              AND b.slot.id IN :slotIds
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedByUserIdAndSlotIds(@Param("userId") Long userId,
                                           @Param("slotIds") Collection<Long> slotIds);

    /** 8회권 환불 시 자동취소 대상 — 해당 pass의 미래 BOOKED 예약 */
    @Override
    @Query("""
            SELECT b FROM Booking b
            WHERE b.passPurchase.id = :passId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
              AND b.slot.startAt > :now
            """)
    List<Booking> findFutureBookedPassBookings(@Param("passId") Long passId,
                                               @Param("now") LocalDateTime now);

    /** D-1 / 당일 리마인드 공용 — LEFT JOIN FETCH guest (member booking 포함, detached 후 LAZY 로딩 방지) */
    @Override
    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.guest
            WHERE b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
              AND b.slot.startAt >= :start
              AND b.slot.startAt < :end
            """)
    List<Booking> findBookedInRange(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /** 관리자 — 날짜 범위 내 예약 전체 조회 (guest nullable, class, slot eager fetch) */
    @Override
    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.slot.startAt >= :start AND b.slot.startAt < :end
            ORDER BY b.slot.startAt ASC
            """)
    List<Booking> findAllInRange(@Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
}
