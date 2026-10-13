package com.personal.happygallery.application.customer.port.in;

import java.util.List;

/**
 * 비회원 → 회원 기록 인수(guest claim) 유스케이스.
 */
public interface GuestClaimUseCase {

    ClaimPreview preview(Long userId);

    ClaimPreview verifyPhoneAndPreview(Long userId, String verificationCode);

    ClaimResult claim(Long userId, List<Long> orderIds, List<Long> bookingIds);

    record ClaimPreview(boolean phoneVerified,
                        List<GuestOrderSummary> orders,
                        List<GuestBookingSummary> bookings) {
        public ClaimPreview {
            orders = orders == null ? List.of() : List.copyOf(orders);
            bookings = bookings == null ? List.of() : List.copyOf(bookings);
        }
    }

    record ClaimResult(int claimedOrderCount, int claimedBookingCount) {}
}
