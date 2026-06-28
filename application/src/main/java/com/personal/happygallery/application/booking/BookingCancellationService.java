package com.personal.happygallery.application.booking;

/**
 * Pass 환불 흐름이 Booking 내부 구현을 직접 알지 않고 연동 예약을 일괄 취소하게 하는 내부 협력 서비스.
 */
public interface BookingCancellationService {

    /**
     * 특정 8회권에 연결된 미래 BOOKED 예약을 모두 취소한다.
     *
     * @param passId 8회권 ID
     * @return 취소된 예약 건수
     */
    int cancelLinkedBookings(Long passId);
}
