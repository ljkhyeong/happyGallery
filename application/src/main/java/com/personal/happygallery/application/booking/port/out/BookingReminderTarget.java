package com.personal.happygallery.application.booking.port.out;

/** 예약 리마인더 outbox 생성에 필요한 최소 수신자 정보. */
public record BookingReminderTarget(Long bookingId, Long userId, Long guestId) {}
