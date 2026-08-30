package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.application.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingStateProbe {

    private final BookingReaderPort bookingReaderPort;
    private final BookingRepository bookingRepository;
    private final BookingHistoryPort bookingHistoryPort;
    private final RefundRepository refundRepository;
    private final SlotReaderPort slotReaderPort;

    public BookingStateProbe(BookingReaderPort bookingReaderPort,
                             BookingRepository bookingRepository,
                             BookingHistoryPort bookingHistoryPort,
                             RefundRepository refundRepository,
                             SlotReaderPort slotReaderPort) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingRepository = bookingRepository;
        this.bookingHistoryPort = bookingHistoryPort;
        this.refundRepository = refundRepository;
        this.slotReaderPort = slotReaderPort;
    }

    public Booking getBooking(Long bookingId) {
        return bookingReaderPort.findById(bookingId).orElseThrow();
    }

    public Slot getSlot(Long slotId) {
        return slotReaderPort.findById(slotId).orElseThrow();
    }

    public long bookingCount() {
        return bookingRepository.count();
    }

    public long bookingHistoryCountByBookingId(Long bookingId) {
        return bookingHistoryPort.countByBookingId(bookingId);
    }

    public List<Refund> refunds() {
        return refundRepository.findAll();
    }

    public long refundCount() {
        return refundRepository.count();
    }
}
