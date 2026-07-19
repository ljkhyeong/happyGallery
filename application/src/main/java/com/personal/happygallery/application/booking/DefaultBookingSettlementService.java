package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.booking.BalanceStatus;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DefaultBookingSettlementService implements BookingSettlementUseCase {

    private final BookingReaderPort bookingReaderPort;
    private final BookingStorePort bookingStorePort;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    DefaultBookingSettlementService(BookingReaderPort bookingReaderPort,
                                    BookingStorePort bookingStorePort,
                                    BookingSupport bookingSupport,
                                    Clock clock) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingStorePort = bookingStorePort;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    @Override
    public Booking markBalancePaid(Long bookingId) {
        Booking booking = findBooking(bookingId);
        BalanceStatus previousBalanceStatus = booking.getBalanceStatus();
        boolean wasArrears = booking.isArrearsFlag();
        booking.markBalancePaid(LocalDateTime.now(clock));
        if (previousBalanceStatus != booking.getBalanceStatus()) {
            bookingSupport.recordHistory(
                    booking, BookingHistoryAction.BALANCE_PAID, null, null, "ADMIN", null);
        }
        if (wasArrears && !booking.isArrearsFlag()) {
            bookingSupport.recordHistory(
                    booking, BookingHistoryAction.ARREARS_CLEARED, null, null, "ADMIN", null);
        }
        return bookingStorePort.save(booking);
    }

    @Override
    public Booking updateArrears(Long bookingId, boolean arrears) {
        Booking booking = findBooking(bookingId);
        boolean previousArrears = booking.isArrearsFlag();
        booking.updateArrears(arrears);
        if (previousArrears != booking.isArrearsFlag()) {
            BookingHistoryAction action = booking.isArrearsFlag()
                    ? BookingHistoryAction.ARREARS_MARKED
                    : BookingHistoryAction.ARREARS_CLEARED;
            bookingSupport.recordHistory(booking, action, null, null, "ADMIN", null);
        }
        return bookingStorePort.save(booking);
    }

    @Override
    public Booking complete(Long bookingId) {
        Booking booking = findBooking(bookingId);
        booking.complete(LocalDateTime.now(clock));
        bookingSupport.recordHistory(
                booking, BookingHistoryAction.COMPLETED, booking.getSlot(), null, "ADMIN", null);
        return bookingStorePort.save(booking);
    }

    private Booking findBooking(Long bookingId) {
        return bookingReaderPort.findById(bookingId)
                .orElseThrow(NotFoundException.supplier("예약"));
    }
}
