package com.personal.happygallery.support;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BookingStateProbe {

    private final BookingRepository bookingRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final RefundRepository refundRepository;
    private final SlotRepository slotRepository;

    public BookingStateProbe(BookingRepository bookingRepository,
                             BookingHistoryRepository bookingHistoryRepository,
                             RefundRepository refundRepository,
                             SlotRepository slotRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.refundRepository = refundRepository;
        this.slotRepository = slotRepository;
    }

    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow();
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
