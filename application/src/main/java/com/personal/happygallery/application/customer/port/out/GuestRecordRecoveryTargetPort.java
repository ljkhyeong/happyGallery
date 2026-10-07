package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;
import java.util.List;

/** 비회원 접근 권한을 재발급할 주문·예약을 조회한다. */
public interface GuestRecordRecoveryTargetPort {

    int replaceOrderAccessTokens(Long guestId, String accessTokenHash);

    int replaceBookingAccessTokens(Long guestId, String accessTokenHash);

    /** 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (createdAt DESC, id DESC)다. */
    List<Order> findOrdersByAccessToken(
            String accessTokenHash, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    /** 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (createdAt DESC, id DESC)다. */
    List<Booking> findBookingsByAccessToken(
            String accessTokenHash, LocalDateTime cursorCreatedAt, Long cursorId, int limit);
}
