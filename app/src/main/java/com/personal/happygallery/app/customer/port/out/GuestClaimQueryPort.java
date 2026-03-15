package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.Collection;
import java.util.List;

/**
 * guest claim 유스케이스 전용 조회 포트.
 *
 * <p>claim preview와 claim 실행에 필요한 주문/예약/이용권 조회를 제공한다.
 */
public interface GuestClaimQueryPort {

    List<Order> findOrdersByGuestId(Long guestId);

    List<Order> findOrdersByIds(Collection<Long> ids);

    List<Booking> findBookingsByGuestId(Long guestId);

    List<Booking> findBookingsByIds(Collection<Long> ids);

    List<PassPurchase> findPassPurchasesByGuestId(Long guestId);

    List<PassPurchase> findPassPurchasesByIds(Collection<Long> ids);
}
