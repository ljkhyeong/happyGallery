package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.util.Collection;
import java.util.List;

/**
 * guest claim 유스케이스의 귀속 대상 조회 포트.
 *
 * <p>claim preview와 claim 실행에 필요한 주문/예약/이용권 조회를 제공한다.
 */
public interface GuestClaimTargetPort {

    List<Order> findOrdersByGuestId(Long guestId, int limit);

    List<Order> findOrdersByIds(Collection<Long> ids);

    List<Booking> findBookingsByGuestId(Long guestId, int limit);

    List<Booking> findBookingsByIds(Collection<Long> ids);

    boolean existsBookedByUserIdAndSlotIds(Long userId, Collection<Long> slotIds);
}
