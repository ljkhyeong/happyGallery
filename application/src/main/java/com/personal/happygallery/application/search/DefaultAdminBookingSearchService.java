package com.personal.happygallery.application.search;

import com.personal.happygallery.application.customer.GuestPhoneProtector;
import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.search.port.out.AdminBookingSearchPort;
import com.personal.happygallery.application.search.port.out.AdminBookingSearchResult;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminBookingSearchService implements AdminBookingSearchUseCase {

    private final AdminBookingSearchPort searchPort;
    private final GuestPhoneProtector guestPhoneProtector;

    DefaultAdminBookingSearchService(AdminBookingSearchPort searchPort,
                                     GuestPhoneProtector guestPhoneProtector) {
        this.searchPort = searchPort;
        this.guestPhoneProtector = guestPhoneProtector;
    }

    @Override
    public OffsetPage<AdminBookingSearchRow> search(BookingStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                     String keyword, int page, int size) {
        return AdminSearchHelper.search(
                searchPort, status, dateFrom, dateTo, keyword, page, size, this::toResponse);
    }

    private AdminBookingSearchRow toResponse(AdminBookingSearchResult result) {
        String bookerPhone = result.guestPhoneEnc() == null
                ? result.memberPhone()
                : guestPhoneProtector.decryptEncryptedPhone(result.guestPhoneEnc());
        return new AdminBookingSearchRow(
                result.bookingId(),
                result.bookingNumber(),
                result.bookerType(),
                result.bookerName(),
                bookerPhone,
                result.className(),
                result.startAt(),
                result.endAt(),
                result.status(),
                result.depositAmount(),
                result.balanceAmount(),
                result.passBooking(),
                result.createdAt());
    }
}
