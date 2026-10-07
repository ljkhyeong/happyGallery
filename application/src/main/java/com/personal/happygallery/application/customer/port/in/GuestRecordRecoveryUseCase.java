package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 휴대폰 번호 인증을 거쳐 만료된 비회원 주문·예약 접근 권한을 복구한다. */
public interface GuestRecordRecoveryUseCase {

    RecoveryResult recover(String phone, String verificationCode);

    CursorPage<RecoveredOrder> listRecoveredOrders(
            String accessToken, String cursor, int size);

    CursorPage<RecoveredBooking> listRecoveredBookings(
            String accessToken, String cursor, int size);

    /** 새 복구 토큰. 복구한 주문·예약은 커서 페이지 조회로 읽는다. */
    record RecoveryResult(String accessToken, Instant expiresAt) {}

    record RecoveredOrder(Long orderId, String status, long totalAmount, OffsetDateTime createdAt) {
        public static RecoveredOrder from(Order order) {
            return new RecoveredOrder(
                    order.getId(),
                    order.getStatus().name(),
                    order.getTotalAmount(),
                    order.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
    }

    record RecoveredBooking(Long bookingId,
                            String status,
                            String className,
                            LocalDateTime startAt,
                            LocalDateTime endAt) {
        public static RecoveredBooking from(Booking booking) {
            return new RecoveredBooking(
                    booking.getId(),
                    booking.getStatus().name(),
                    booking.getBookingClass().getName(),
                    booking.getSlot().getStartAt(),
                    booking.getSlot().getEndAt());
        }
    }
}
