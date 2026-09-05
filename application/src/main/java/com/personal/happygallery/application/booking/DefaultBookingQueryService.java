package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.payment.PaymentReceiptQuery;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.application.customer.port.out.MemberHistoryReaderPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.booking.Booking;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultBookingQueryService implements BookingQueryUseCase {

    private final BookingSupport bookingSupport;
    private final BookingReaderPort bookingReaderPort;
    private final RefundPort refundPort;
    private final PaymentReceiptQuery receiptQuery;
    private final MemberHistoryReaderPort memberHistoryReader;

    public DefaultBookingQueryService(BookingSupport bookingSupport,
                                      BookingReaderPort bookingReaderPort,
                                      RefundPort refundPort,
                                      PaymentReceiptQuery receiptQuery,
                                      MemberHistoryReaderPort memberHistoryReader) {
        this.bookingSupport = bookingSupport;
        this.bookingReaderPort = bookingReaderPort;
        this.refundPort = refundPort;
        this.receiptQuery = receiptQuery;
        this.memberHistoryReader = memberHistoryReader;
    }

    /**
     * access_token으로 비회원 예약을 조회한다.
     * bookingId + accessToken 두 조건이 모두 일치해야 한다.
     */
    @Override
    public BookingDetail getBookingByToken(Long bookingId, String accessToken) {
        return detail(bookingSupport.findByToken(bookingId, accessToken));
    }

    /** 회원 — 자기 예약 목록 조회 */
    @Override
    public List<Booking> listMyBookings(Long userId) {
        return listMyBookings(userId, null, PageParams.MAX_SIZE).content();
    }

    @Override
    public CursorPage<Booking> listMyBookings(Long userId, BookingHistoryQuery query, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        if (query.isDefault()) return listMyBookings(userId, cursor, pageSize);
        return memberHistoryReader.findBookings(userId, query, cursor, pageSize);
    }

    @Override
    public CursorPage<Booking> listMyBookings(Long userId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        List<Booking> bookings;
        if (cursor == null) {
            bookings = bookingReaderPort.findByUserIdWithDetails(userId, fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            bookings = bookingReaderPort.findByUserIdWithDetailsAfter(
                    userId, cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        return CursorPage.of(
                bookings,
                pageSize,
                booking -> CursorUtils.encode(
                        booking.getCreatedAt(), booking.getId()));
    }

    /** 회원 — 자기 예약 상세 조회 */
    @Override
    public BookingDetail findMyBooking(Long id, Long userId) {
        Booking booking = bookingReaderPort.findByIdAndUserIdWithDetails(id, userId)
                .orElseThrow(NotFoundException.supplier("예약"));
        return detail(booking);
    }

    private BookingDetail detail(Booking booking) {
        return new BookingDetail(
                booking,
                refundPort.findLatestByBookingId(booking.getId()).orElse(null),
                receiptQuery.findReceipt(PaymentContext.BOOKING, booking.getId()));
    }
}
