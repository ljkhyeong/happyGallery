package com.personal.happygallery.infra.customer;

import com.personal.happygallery.app.customer.port.out.GuestClaimQueryPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.order.OrderRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class JpaGuestClaimQueryAdapter implements GuestClaimQueryPort {

    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final PassPurchaseRepository passPurchaseRepository;

    JpaGuestClaimQueryAdapter(OrderRepository orderRepository,
                              BookingRepository bookingRepository,
                              PassPurchaseRepository passPurchaseRepository) {
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.passPurchaseRepository = passPurchaseRepository;
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
    public List<PassPurchase> findPassPurchasesByGuestId(Long guestId) {
        return passPurchaseRepository.findByGuestIdOrderByPurchasedAtDesc(guestId);
    }

    @Override
    public List<PassPurchase> findPassPurchasesByIds(Collection<Long> ids) {
        return passPurchaseRepository.findAllById(ids);
    }
}
