package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.Refund;

/** 공방 사정으로 예약 또는 비활성 슬롯의 수업 전체를 취소하고 고객 보상을 시작하는 관리자 유스케이스. */
public interface AdminBookingCancelUseCase {

    AdminCancelResult cancel(AdminCancelCommand command);

    CancelSessionResult cancelSession(CancelSessionCommand command);

    record AdminCancelCommand(Long bookingId, Long adminId, String reason) {}

    record CancelSessionCommand(Long slotId, Long adminId, String reason) {}

    record AdminCancelResult(
            Booking booking,
            boolean passCreditRestored,
            Refund refund,
            boolean balanceSettlementRequired,
            boolean manualCompensationRequired
    ) {}

    record CancelSessionResult(
            int canceledBookings,
            int passCreditsRestored,
            int depositRefundsRequested,
            int balanceSettlementsRequired,
            int manualCompensationsRequired
    ) {}
}
