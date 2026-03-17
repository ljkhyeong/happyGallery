package com.personal.happygallery.app.booking;

import com.personal.happygallery.app.booking.port.out.BookingReaderPort;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.user.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminBookingQueryService {

    private final BookingReaderPort bookingReaderPort;
    private final UserReaderPort userReaderPort;

    public AdminBookingQueryService(BookingReaderPort bookingReaderPort,
                                     UserReaderPort userReaderPort) {
        this.bookingReaderPort = bookingReaderPort;
        this.userReaderPort = userReaderPort;
    }

    /**
     * 관리자 예약 목록 조회 — 날짜 기준, 선택적 상태 필터.
     * member booking의 User 정보를 batch fetch하여 DTO에 포함한다.
     */
    public List<AdminBookingResponse> listBookings(LocalDate date, BookingStatus status) {
        List<Booking> bookings = bookingReaderPort.findAllInRange(
                date.atStartOfDay(), date.atTime(LocalTime.MAX));

        Map<Long, User> userMap = resolveUsers(bookings);

        return bookings.stream()
                .filter(b -> status == null || b.getStatus() == status)
                .map(b -> AdminBookingResponse.from(b, userMap.get(b.getUserId())))
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
        return userReaderPort.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
