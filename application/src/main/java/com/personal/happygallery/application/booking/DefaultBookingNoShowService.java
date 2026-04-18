package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingNoShowUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingHistoryAction;
import com.personal.happygallery.domain.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class DefaultBookingNoShowService implements BookingNoShowUseCase {

    private final BookingReaderPort bookingReader;
    private final BookingStorePort bookingStore;
    private final BookingSupport bookingSupport;

    DefaultBookingNoShowService(BookingReaderPort bookingReader,
                                BookingStorePort bookingStore,
                                BookingSupport bookingSupport) {
        this.bookingReader = bookingReader;
        this.bookingStore = bookingStore;
        this.bookingSupport = bookingSupport;
    }

    /**
     * 결석(NO_SHOW) 처리. 관리자 수동 호출.
     *
     * <p>크레딧은 예약 생성 시 이미 USE ledger로 소멸되었으므로 추가 크레딧 변동 없음.
     * 상태를 NO_SHOW로 전이하고 이력을 기록한다.
     *
     * @param bookingId 결석 처리할 예약 ID
     */
    @Override
    public Booking markNoShow(Long bookingId) {
        Booking booking = bookingReader.findById(bookingId)
                .orElseThrow(NotFoundException.supplier("예약"));

        bookingSupport.recordHistory(booking, BookingHistoryAction.NO_SHOW,
                booking.getSlot(), null, "ADMIN", null);

        booking.markNoShow();
        return bookingStore.save(booking);
    }
}
