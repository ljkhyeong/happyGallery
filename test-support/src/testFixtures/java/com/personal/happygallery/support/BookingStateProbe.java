package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingStateProbe {

    private final BookingReaderPort bookingReaderPort;
    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final RefundRepository refundRepository;
    private final SlotRepository slotRepository;

    public BookingStateProbe(BookingReaderPort bookingReaderPort,
                             BookingRepository bookingRepository,
                             BookingHistoryRepository bookingHistoryRepository,
                             RefundRepository refundRepository,
                             SlotRepository slotRepository) {
        this.bookingReaderPort = bookingReaderPort;
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.refundRepository = refundRepository;
        this.slotRepository = slotRepository;
    }

    public Booking getBooking(Long bookingId) {
        return bookingReaderPort.findById(bookingId).orElseThrow();
    }

    public Slot getSlot(Long slotId) {
        return slotRepository.findById(slotId).orElseThrow();
    }

    public long bookingCount() {
        return bookingRepository.count();
    }

    public long bookingHistoryCountByBookingId(Long bookingId) {
        return bookingHistoryRepository.countByBookingId(bookingId);
    }

    public List<Refund> refunds() {
        return refundRepository.findAll();
    }

    public long refundCount() {
        return refundRepository.count();
    }
}
