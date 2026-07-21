package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.util.List;

/** 비회원 접근 권한을 재발급할 주문·예약을 조회한다. */
public interface GuestRecordRecoveryTargetPort {

    List<Order> findOrdersByGuestId(Long guestId);

    List<Booking> findBookingsByGuestId(Long guestId);
}
