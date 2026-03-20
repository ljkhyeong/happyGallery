package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.booking.port.out.BookingHistoryPort;
import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.booking.port.out.BookingStorePort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistory;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.app.pass.port.in.PassNoShowUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPassNoShowService implements PassNoShowUseCase {

    private final BookingReaderPort bookingReader;
    private final BookingStorePort bookingStore;
    private final BookingHistoryPort bookingHistoryPort;

    public DefaultPassNoShowService(BookingReaderPort bookingReader,
                                    BookingStorePort bookingStore,
                                    BookingHistoryPort bookingHistoryPort) {
        this.bookingReader = bookingReader;
        this.bookingStore = bookingStore;
        this.bookingHistoryPort = bookingHistoryPort;
    }

    /**
     * 결석(NO_SHOW) 처리. 관리자 수동 호출.
     *
     * <p>크레딧은 예약 생성 시 이미 USE ledger로 소멸되었으므로 추가 크레딧 변동 없음.
     * 상태를 NO_SHOW로 전이하고 이력을 기록한다.
     *
     * @param bookingId 결석 처리할 예약 ID
     */
    public Booking markNoShow(Long bookingId) {
        Booking booking = bookingReader.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("예약"));

        bookingHistoryPort.save(
                new BookingHistory(booking, BookingHistoryAction.NO_SHOW,
                        booking.getSlot(), null, "ADMIN", null));

        booking.markNoShow();
        return bookingStore.save(booking);
    }
}
