package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.application.customer.port.out.GuestRecordRecoveryTargetPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class JpaGuestRecordRecoveryTargetAdapter implements GuestRecordRecoveryTargetPort {

    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;

    JpaGuestRecordRecoveryTargetAdapter(OrderRepository orderRepository,
                                        BookingRepository bookingRepository) {
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<Order> findOrdersByGuestId(Long guestId) {
        return orderRepository.findByGuestIdOrderByCreatedAtDesc(guestId);
    }

    @Override
    public List<Booking> findBookingsByGuestId(Long guestId) {
        return bookingRepository.findByGuestIdWithDetails(guestId);
    }
}
