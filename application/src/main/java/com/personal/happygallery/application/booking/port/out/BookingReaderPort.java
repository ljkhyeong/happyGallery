package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingReaderPort {

    Optional<Booking> findById(Long id);

    Optional<Booking> findByIdForUpdate(Long id);

    Optional<Booking> findByIdAndAccessTokenForUpdate(Long id, String accessToken);

    Optional<Booking> findDetailByIdAndAccessToken(Long id, String accessToken);

    List<Booking> findByUserIdWithDetails(Long userId, int limit);

    List<Booking> findByUserIdWithDetailsAfter(
            Long userId, LocalDateTime createdAt, Long id, int limit);

    Optional<Booking> findByIdAndUserIdWithDetails(Long id, Long userId);

    List<Long> findBookedPassIdsBySlotId(Long slotId);

    List<Booking> findBookedBySlotIdForUpdate(Long slotId);

    boolean existsBookedBySlotIdAndOwnerPhoneHmac(Long slotId, String ownerPhoneHmac);

    boolean existsBookedBySlotIdAndOwnerPhoneHmacAndIdNot(
            Long slotId, String ownerPhoneHmac, Long excludeBookingId);

    List<Booking> findFutureBookedPassBookings(Long passId, LocalDateTime now);

    List<Booking> findAllInRange(LocalDateTime start, LocalDateTime end);

    List<Booking> findByStatusInRange(LocalDateTime start, LocalDateTime end, BookingStatus status);
}
