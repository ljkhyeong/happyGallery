package com.personal.happygallery.app.customer.port.in;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 비회원 → 회원 기록 인수(guest claim) 유스케이스.
 */
public interface GuestClaimUseCase {

    ClaimPreview preview(Long userId);

    ClaimPreview verifyPhoneAndPreview(Long userId, String verificationCode);

    ClaimResult claim(Long userId, List<Long> orderIds, List<Long> bookingIds);

    record ClaimPreview(boolean phoneVerified,
                        List<ClaimOrderSummary> orders,
                        List<ClaimBookingSummary> bookings) {}

    record ClaimOrderSummary(Long orderId, String status, long totalAmount, LocalDateTime createdAt) {
        public static ClaimOrderSummary from(Order order) {
            return new ClaimOrderSummary(order.getId(), order.getStatus().name(),
                    order.getTotalAmount(), order.getCreatedAt());
        }
    }

    record ClaimBookingSummary(Long bookingId, String status, String className,
                               LocalDateTime startAt, LocalDateTime endAt) {
        public static ClaimBookingSummary from(Booking booking) {
            return new ClaimBookingSummary(booking.getId(), booking.getStatus().name(),
                    booking.getBookingClass().getName(), booking.getSlot().getStartAt(),
                    booking.getSlot().getEndAt());
        }
    }

    record ClaimResult(int claimedOrderCount, int claimedBookingCount) {}
}
