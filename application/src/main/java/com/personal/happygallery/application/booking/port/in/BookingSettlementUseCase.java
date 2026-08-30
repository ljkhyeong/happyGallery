package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;

/** 관리자 예약 잔금 정산과 수업 완료 유스케이스. */
public interface BookingSettlementUseCase {

    Booking markBalancePaid(Long bookingId, Long adminId);

    Booking updateArrears(Long bookingId, boolean arrears, Long adminId);

    Booking complete(Long bookingId, Long adminId);
}
