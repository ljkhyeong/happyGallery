package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.AdminBookingResponse;
import com.personal.happygallery.application.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.application.booking.port.out.BookingReaderPort;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
@Transactional(readOnly = true)
public class DefaultAdminBookingQueryService implements AdminBookingQueryUseCase {

    private final BookingReaderPort bookingReaderPort;
    private final UserReaderPort userReaderPort;
    private final GuestPersonalDataProtector guestPersonalDataProtector;

    public DefaultAdminBookingQueryService(BookingReaderPort bookingReaderPort,
                                           UserReaderPort userReaderPort,
                                           GuestPersonalDataProtector guestPersonalDataProtector) {
        this.bookingReaderPort = bookingReaderPort;
        this.userReaderPort = userReaderPort;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
    }

    /**
     * 관리자 예약 목록 조회 — 날짜 기준, 선택적 상태 필터.
     * member booking의 User 정보를 batch fetch하여 DTO에 포함한다.
     */
    @Override
    public List<AdminBookingResponse> listBookings(LocalDate date, BookingStatus status) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Booking> bookings = status == null
                ? bookingReaderPort.findAllInRange(start, end)
                : bookingReaderPort.findByStatusInRange(start, end, status);

        Map<Long, User> userMap = resolveUsers(bookings);

        return bookings.stream()
                .map(booking -> {
                    Long userId = booking.getUserId();
                    if (userId != null) {
                        return AdminBookingResponse.fromMember(booking, userMap.get(userId));
                    }
                    return AdminBookingResponse.fromGuest(
                            booking,
                            guestPersonalDataProtector.decryptName(booking.getGuest()),
                            guestPersonalDataProtector.decryptPhone(booking.getGuest()));
                })
                .toList();
    }

    private Map<Long, User> resolveUsers(List<Booking> bookings) {
        List<Long> userIds = bookings.stream()
                .map(Booking::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userReaderPort.findAllByIdForAdminHistory(userIds).stream()
                .collect(toMap(User::getId, Function.identity()));
    }
}
