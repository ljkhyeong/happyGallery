package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.Booking;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** 비회원 예약 조회 — bookingId + accessToken 두 조건 모두 만족해야 함 */
    Optional<Booking> findByIdAndAccessToken(Long id, String accessToken);

    /** 동일 슬롯 + 동일 게스트 중복 예약 확인 */
    boolean existsBySlotIdAndGuestId(Long slotId, Long guestId);
}
