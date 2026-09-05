package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.application.booking.BookingHistoryQuery;
import com.personal.happygallery.application.order.OrderHistoryQuery;
import com.personal.happygallery.application.pass.PassHistoryQuery;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.time.LocalDateTime;

public interface MemberHistoryReaderPort {
    CursorPage<Order> findOrders(Long userId, OrderHistoryQuery query, String cursor, int size);
    CursorPage<Booking> findBookings(Long userId, BookingHistoryQuery query, String cursor, int size);
    CursorPage<PassPurchase> findPasses(Long userId, PassHistoryQuery query, String cursor, int size,
                                      LocalDateTime now);
}
