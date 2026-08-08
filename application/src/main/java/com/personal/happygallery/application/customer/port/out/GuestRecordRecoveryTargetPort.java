package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;
import java.util.List;

/** 비회원 접근 권한을 재발급할 주문·예약을 조회한다. */
public interface GuestRecordRecoveryTargetPort {

    List<Order> findOrdersByGuestId(Long guestId, int limit);

    List<Booking> findBookingsByGuestId(Long guestId, int limit);

    int replaceOrderAccessTokens(Long guestId, String accessTokenHash);

    int replaceBookingAccessTokens(Long guestId, String accessTokenHash);

    List<Order> findOrdersByAccessToken(String accessTokenHash, int limit);

    List<Order> findOrdersByAccessTokenAfter(
            String accessTokenHash, LocalDateTime createdAt, Long id, int limit);

    List<Booking> findBookingsByAccessToken(String accessTokenHash, int limit);

    List<Booking> findBookingsByAccessTokenAfter(
            String accessTokenHash, LocalDateTime createdAt, Long id, int limit);
}
