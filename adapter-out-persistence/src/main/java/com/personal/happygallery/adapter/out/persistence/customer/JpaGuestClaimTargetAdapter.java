package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.application.customer.port.out.GuestClaimTargetPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class JpaGuestClaimTargetAdapter implements GuestClaimTargetPort {

    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;

    JpaGuestClaimTargetAdapter(OrderRepository orderRepository,
                               BookingRepository bookingRepository) {
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<Order> findOrdersByGuestId(Long guestId) {
        return orderRepository.findByGuestIdOrderByCreatedAtDesc(guestId);
    }

    @Override
    public List<Order> findOrdersByIds(Collection<Long> ids) {
        return orderRepository.findAllById(ids);
    }

    @Override
    public List<Booking> findBookingsByGuestId(Long guestId) {
        return bookingRepository.findByGuestIdWithDetails(guestId);
    }

    @Override
    public List<Booking> findBookingsByIds(Collection<Long> ids) {
        return bookingRepository.findAllById(ids);
    }

    @Override
    public boolean existsBookedByUserIdAndSlotIds(Long userId, Collection<Long> slotIds) {
        return bookingRepository.existsBookedByUserIdAndSlotIds(userId, slotIds);
    }
}
