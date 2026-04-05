package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.in.BookingCancellationUseCase;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.booking.Slot;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pass 환불 시 연동 예약 일괄 취소를 처리하는 도메인 서비스.
 *
 * <p>Pass 도메인이 Booking 내부 Port를 직접 참조하지 않도록
 * {@link BookingCancellationUseCase}를 구현한다.
 */
@Service
@Transactional
class DefaultBookingCancellationService implements BookingCancellationUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultBookingCancellationService.class);

    private final BookingReaderPort bookingReader;
    private final BookingStorePort bookingStore;
    private final BookingSlotSupport slotSupport;
    private final BookingSupport bookingSupport;
    private final Clock clock;

    DefaultBookingCancellationService(BookingReaderPort bookingReader,
                                      BookingStorePort bookingStore,
                                      BookingSlotSupport slotSupport,
                                      BookingSupport bookingSupport,
                                      Clock clock) {
        this.bookingReader = bookingReader;
        this.bookingStore = bookingStore;
        this.slotSupport = slotSupport;
        this.bookingSupport = bookingSupport;
        this.clock = clock;
    }

    @Override
    public int cancelLinkedBookings(Long passId) {
        List<Booking> futureBookings = bookingReader.findFuturePassBookings(
                passId, BookingStatus.BOOKED, LocalDateTime.now(clock));

        futureBookings.forEach(booking -> cancelOne(booking, passId));

        return futureBookings.size();
    }

    private void cancelOne(Booking booking, Long passId) {
        booking.cancel();

        Slot slot = slotSupport.releaseSlotCapacity(booking.getSlot().getId());

        bookingSupport.recordHistory(booking, BookingHistoryAction.CANCELED,
                slot, null, "ADMIN", null);

        bookingStore.save(booking);
        log.info("Pass환불 연동 취소 [passId={}, bookingId={}]", passId, booking.getId());
    }
}
