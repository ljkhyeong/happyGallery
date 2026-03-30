package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.SlotReaderPort;
import com.personal.happygallery.app.payment.port.out.RefundPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingStateProbe {

    private final BookingReaderPort bookingReaderPort;
    private final BookingHistoryPort bookingHistoryPort;
    private final RefundPort refundPort;
    private final SlotReaderPort slotReaderPort;

    public BookingStateProbe(BookingReaderPort bookingReaderPort,
                             BookingHistoryPort bookingHistoryPort,
                             RefundPort refundPort,
                             SlotReaderPort slotReaderPort) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingHistoryPort = bookingHistoryPort;
        this.refundPort = refundPort;
        this.slotReaderPort = slotReaderPort;
    }

    public Booking getBooking(Long bookingId) {
        return bookingReaderPort.findById(bookingId).orElseThrow();
    }

    public Slot getSlot(Long slotId) {
        return slotReaderPort.findById(slotId).orElseThrow();
    }

    public long bookingCount() {
        return bookingReaderPort.count();
    }

    public long bookingHistoryCountByBookingId(Long bookingId) {
        return bookingHistoryPort.countByBookingId(bookingId);
    }

    public List<Refund> refunds() {
        return refundPort.findAll();
    }

    public long refundCount() {
        return refundPort.count();
    }
}
