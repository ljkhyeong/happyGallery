package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.infra.booking.BookingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link BookingRepository}(infra) → {@link BookingReaderPort} + {@link BookingStorePort}(app) 브릿지 어댑터.
 */
@Component
class BookingPersistencePortAdapter implements BookingReaderPort, BookingStorePort {

    private final BookingRepository bookingRepository;

    BookingPersistencePortAdapter(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public Optional<Booking> findDetailByIdAndAccessToken(Long id, String accessToken) {
        return bookingRepository.findDetailByIdAndAccessToken(id, accessToken);
    }

    @Override
    public List<Booking> findByUserIdWithDetails(Long userId) {
        return bookingRepository.findByUserIdWithDetails(userId);
    }

    @Override
    public Optional<Booking> findByIdAndUserIdWithDetails(Long id, Long userId) {
        return bookingRepository.findByIdAndUserIdWithDetails(id, userId);
    }

    @Override
    public List<Booking> findByGuestIdWithDetails(Long guestId) {
        return bookingRepository.findByGuestIdWithDetails(guestId);
    }

    @Override
    public boolean existsBySlotIdAndGuestId(Long slotId, Long guestId) {
        return bookingRepository.existsBySlotIdAndGuestId(slotId, guestId);
    }

    @Override
    public boolean existsBySlotIdAndUserId(Long slotId, Long userId) {
        return bookingRepository.existsBySlotIdAndUserId(slotId, userId);
    }

    @Override
    public boolean existsBySlotIdAndGuestIdAndIdNot(Long slotId, Long guestId, Long excludeBookingId) {
        return bookingRepository.existsBySlotIdAndGuestIdAndIdNot(slotId, guestId, excludeBookingId);
    }

    @Override
    public boolean existsBySlotIdAndUserIdAndIdNot(Long slotId, Long userId, Long excludeBookingId) {
        return bookingRepository.existsBySlotIdAndUserIdAndIdNot(slotId, userId, excludeBookingId);
    }

    @Override
    public List<Booking> findFuturePassBookings(Long passId, BookingStatus status, LocalDateTime now) {
        return bookingRepository.findFuturePassBookings(passId, status, now);
    }

    @Override
    public List<Booking> findBookingsInRange(BookingStatus status, LocalDateTime start, LocalDateTime end) {
        return bookingRepository.findBookingsInRange(status, start, end);
    }

    @Override
    public List<Booking> findAllInRange(LocalDateTime start, LocalDateTime end) {
        return bookingRepository.findAllInRange(start, end);
    }

    @Override
    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }
}
