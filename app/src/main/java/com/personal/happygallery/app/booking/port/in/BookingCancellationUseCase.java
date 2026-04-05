package com.personal.happygallery.app.booking.port.in;

/**
 * Pass 도메인이 연동 예약을 일괄 취소할 때 사용하는 인바운드 포트.
 *
 * <p>Pass가 Booking 내부 구현(BookingStorePort, SlotStorePort 등)을
 * 직접 알지 않아도 연동 예약 취소가 가능하도록 추상화한다.
 */
public interface BookingCancellationUseCase {

    /**
     * 특정 8회권에 연결된 미래 BOOKED 예약을 모두 취소한다.
     *
     * @param passId 8회권 ID
     * @return 취소된 예약 건수
     */
    int cancelLinkedBookings(Long passId);
}
