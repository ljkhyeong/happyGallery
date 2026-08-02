package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long>, BookingReaderPort {

    @Override Optional<Booking> findById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Booking b
            SET b.ownerPhoneHmac = :ownerPhoneHmac,
                b.version = b.version + 1
            WHERE b.userId = :userId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    int updateBookedOwnerPhoneHmacByUserId(
            @Param("userId") Long userId,
            @Param("ownerPhoneHmac") String ownerPhoneHmac);

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

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.userId = :userId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedByUserId(@Param("userId") Long userId);

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

    /** 비회원 예약 상세 조회 (슬롯 시작 시간 내림차순). */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.guest.id = :guestId
            ORDER BY b.slot.startAt DESC
            """)
    List<Booking> findByGuestIdWithDetails(@Param("guestId") Long guestId, Pageable pageable);

    @Override
    default List<Booking> findByGuestIdWithDetails(Long guestId) {
        return findByGuestIdWithDetails(guestId, Pageable.unpaged());
    }

    /** 운영자 수업 취소가 클래스·슬롯보다 먼저 잠글 8회권 ID를 PK 순으로 조회한다. */
    @Override
    @Query("""
            SELECT DISTINCT b.passPurchase.id FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
              AND b.passPurchase IS NOT NULL
            ORDER BY b.passPurchase.id
            """)
    List<Long> findBookedPassIdsBySlotId(@Param("slotId") Long slotId);

    /** 운영자 수업 취소 대상 — 슬롯 잠금 뒤 최신 BOOKED 예약 행만 ID 순으로 잠금 조회한다. */
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            ORDER BY b.id
            """)
    List<Booking> findBookedBySlotIdForUpdate(@Param("slotId") Long slotId);

    /** guest claim 실행에 필요한 소유자와 슬롯을 한 번에 조회한다. */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.guest
            JOIN FETCH b.slot
            WHERE b.id IN :ids
            """)
    List<Booking> findClaimTargetsByIdIn(@Param("ids") Collection<Long> ids);

    /** 동일 슬롯에 같은 전화번호 소유자의 활성 예약이 있는지 확인한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.ownerPhoneHmac = :ownerPhoneHmac
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedBySlotIdAndOwnerPhoneHmac(
            @Param("slotId") Long slotId,
            @Param("ownerPhoneHmac") String ownerPhoneHmac);

    /** 예약 변경 시 자기 자신을 제외하고 같은 전화번호 소유자의 활성 예약이 있는지 확인한다. */
    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.slot.id = :slotId
              AND b.ownerPhoneHmac = :ownerPhoneHmac
              AND b.id <> :excludeBookingId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.BOOKED
            """)
    boolean existsBookedBySlotIdAndOwnerPhoneHmacAndIdNot(
            @Param("slotId") Long slotId,
            @Param("ownerPhoneHmac") String ownerPhoneHmac,
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
            ORDER BY b.slot.id
            """)
    List<Booking> findFutureBookedPassBookings(@Param("passId") Long passId,
                                               @Param("now") LocalDateTime now);

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

    /** 관리자 — 날짜 범위와 상태로 예약 조회 */
    @Override
    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.guest
            JOIN FETCH b.bookingClass
            JOIN FETCH b.slot
            WHERE b.slot.startAt >= :start
              AND b.slot.startAt < :end
              AND b.status = :status
            ORDER BY b.slot.startAt ASC
            """)
    List<Booking> findByStatusInRange(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end,
                                      @Param("status") BookingStatus status);
}
