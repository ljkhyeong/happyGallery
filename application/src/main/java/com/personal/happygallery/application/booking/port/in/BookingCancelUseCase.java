package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;

/**
 * 예약 취소 유스케이스.
 *
 * <p>비회원(access-token) / 회원(userId) 두 경로를 지원한다.
 */
public interface BookingCancelUseCase {

    CancelResult cancelBooking(Long bookingId, String accessToken);

    CancelResult cancelMemberBooking(Long bookingId, Long userId);

    ParticipantReductionResult reduceGuestBookingParticipants(
            Long bookingId, String accessToken, int participantCount);

    ParticipantReductionResult reduceMemberBookingParticipants(
            Long bookingId, Long userId, int participantCount);

    record CancelResult(
            Booking booking,
            boolean refundable,
            Refund refund,
            boolean manualCompensationRequired
    ) {}

    record ParticipantReductionResult(
            Booking booking,
            int canceledParticipantCount,
            Refund refund
    ) {}
}
