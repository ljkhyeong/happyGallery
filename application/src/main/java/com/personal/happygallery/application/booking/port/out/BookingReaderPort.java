package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Booking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingReaderPort {

    Optional<Booking> findById(Long id);

    long count();

    Optional<Booking> findDetailByIdAndAccessToken(Long id, String accessToken);

    List<Booking> findByUserIdWithDetails(Long userId);

    Optional<Booking> findByIdAndUserIdWithDetails(Long id, Long userId);

    List<Booking> findByGuestIdWithDetails(Long guestId);

    boolean existsBookedBySlotIdAndGuestId(Long slotId, Long guestId);

    boolean existsBookedBySlotIdAndUserId(Long slotId, Long userId);

    boolean existsBookedBySlotIdAndGuestIdAndIdNot(Long slotId, Long guestId, Long excludeBookingId);

    boolean existsBookedBySlotIdAndUserIdAndIdNot(Long slotId, Long userId, Long excludeBookingId);

    List<Booking> findFutureBookedPassBookings(Long passId, LocalDateTime now);

    List<Booking> findBookedInRange(LocalDateTime start, LocalDateTime end);

    List<Booking> findAllInRange(LocalDateTime start, LocalDateTime end);
}
