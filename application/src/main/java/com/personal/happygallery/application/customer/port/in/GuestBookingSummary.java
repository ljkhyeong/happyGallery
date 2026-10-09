package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import java.time.LocalDateTime;

/** 비회원 기록 인수 미리보기와 기록 복구 목록이 함께 쓰는 비회원 예약 요약. */
public record GuestBookingSummary(Long bookingId,
                                  BookingStatus status,
                                  String className,
                                  LocalDateTime startAt,
                                  LocalDateTime endAt) {

    public static GuestBookingSummary from(Booking booking) {
        return new GuestBookingSummary(
                booking.getId(),
                booking.getStatus(),
                booking.getBookingClass().getName(),
                booking.getSlot().getStartAt(),
                booking.getSlot().getEndAt());
    }
}
