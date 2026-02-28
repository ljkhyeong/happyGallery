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

    /** 동일 슬롯 + 동일 게스트 중복 예약 확인 */
    boolean existsBySlotIdAndGuestId(Long slotId, Long guestId);

    /** 동일 슬롯 + 동일 게스트 중복 예약 확인 — 특정 booking 제외 (변경 시 자기 자신 제외용) */
    boolean existsBySlotIdAndGuestIdAndIdNot(Long slotId, Long guestId, Long excludeBookingId);

    /** 8회권 환불 시 자동취소 대상 — 해당 pass의 미래 BOOKED 예약 */
    @Query("SELECT b FROM Booking b WHERE b.passPurchase.id = :passId AND b.status = :status AND b.slot.startAt > :now")
    List<Booking> findFuturePassBookings(@Param("passId") Long passId,
                                         @Param("status") BookingStatus status,
                                         @Param("now") LocalDateTime now);
}
