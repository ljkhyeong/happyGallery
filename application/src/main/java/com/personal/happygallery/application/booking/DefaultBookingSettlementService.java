package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.notification.ReviewNotificationPublisher;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingHistoryActor;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DefaultBookingSettlementService implements BookingSettlementUseCase {

    private final BookingStorePort bookingStorePort;
    private final BookingSupport bookingSupport;
    private final ReviewNotificationPublisher reviewNotificationPublisher;
    private final Clock clock;

    DefaultBookingSettlementService(BookingStorePort bookingStorePort,
                                    BookingSupport bookingSupport,
                                    ReviewNotificationPublisher reviewNotificationPublisher,
                                    Clock clock) {
        this.bookingStorePort = bookingStorePort;
        this.bookingSupport = bookingSupport;
        this.reviewNotificationPublisher = reviewNotificationPublisher;
        this.clock = clock;
    }

    @Override
    public Booking markBalancePaid(Long bookingId, Long adminId) {
        Booking booking = bookingSupport.findById(bookingId);
        BalanceStatus previousBalanceStatus = booking.getBalanceStatus();
        boolean wasArrears = booking.isArrearsFlag();
        booking.markBalancePaid(LocalDateTime.now(clock));
        if (previousBalanceStatus != booking.getBalanceStatus()) {
            bookingSupport.recordHistory(
                    booking, BookingHistoryAction.BALANCE_PAID, null, null,
                    BookingHistoryActor.ADMIN, adminId, null);
        }
        if (wasArrears && !booking.isArrearsFlag()) {
            bookingSupport.recordHistory(
                    booking, BookingHistoryAction.ARREARS_CLEARED, null, null,
                    BookingHistoryActor.ADMIN, adminId, null);
        }
        return bookingStorePort.save(booking);
    }

    @Override
    public Booking updateArrears(Long bookingId, boolean arrears, Long adminId) {
        Booking booking = bookingSupport.findById(bookingId);
        boolean previousArrears = booking.isArrearsFlag();
        booking.updateArrears(arrears);
        if (previousArrears != booking.isArrearsFlag()) {
            BookingHistoryAction action = booking.isArrearsFlag()
                    ? BookingHistoryAction.ARREARS_MARKED
                    : BookingHistoryAction.ARREARS_CLEARED;
            bookingSupport.recordHistory(
                    booking, action, null, null, BookingHistoryActor.ADMIN, adminId, null);
        }
        return bookingStorePort.save(booking);
    }

    @Override
    public Booking complete(Long bookingId, Long adminId) {
        Booking booking = bookingSupport.findById(bookingId);
        booking.complete(LocalDateTime.now(clock));
        bookingSupport.recordHistory(
                booking, BookingHistoryAction.COMPLETED, booking.getSlot(), null,
                BookingHistoryActor.ADMIN, adminId, null);
        Booking completed = bookingStorePort.save(booking);
        reviewNotificationPublisher.requestForBooking(completed.getUserId(), completed.getId());
        return completed;
    }
}
