package com.personal.happygallery.app.pass.port.in;

import com.personal.happygallery.domain.booking.Booking;

/**
 * 8회권 결석(NO_SHOW) 처리 유스케이스.
 *
 * <p>관리자가 수동으로 결석 처리한다.
 */
public interface PassNoShowUseCase {

    Booking markNoShow(Long bookingId);
}
