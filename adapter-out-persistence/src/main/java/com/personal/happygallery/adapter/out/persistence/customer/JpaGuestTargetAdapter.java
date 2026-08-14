package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.application.customer.port.out.GuestClaimTargetPort;
import com.personal.happygallery.application.customer.port.out.GuestRecordRecoveryTargetPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
class JpaGuestTargetAdapter
        implements GuestClaimTargetPort, GuestRecordRecoveryTargetPort {

    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;

    JpaGuestTargetAdapter(OrderRepository orderRepository,
                          BookingRepository bookingRepository) {
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public List<Order> findOrdersByGuestId(Long guestId, int limit) {
        return orderRepository.findByGuestIdOrderByCreatedAtDescIdDesc(
                guestId, PageRequest.ofSize(limit));
    }

    @Override
    public List<Booking> findBookingsByGuestId(Long guestId, int limit) {
        return bookingRepository.findByGuestIdWithDetails(
                guestId, PageRequest.ofSize(limit));
    }

    @Override
    public List<Order> findOrdersByIds(Collection<Long> ids) {
        return orderRepository.findAllById(ids);
    }

    @Override
    public List<Booking> findBookingsByIds(Collection<Long> ids) {
        return bookingRepository.findClaimTargetsByIdIn(ids);
    }

    @Override
    public boolean existsBookedByUserIdAndSlotIds(
            Long userId, Collection<Long> slotIds) {
        return bookingRepository.existsBookedByUserIdAndSlotIds(userId, slotIds);
    }

    @Override
    public int replaceOrderAccessTokens(Long guestId, String accessTokenHash) {
        return orderRepository.replaceAccessTokenByGuestId(guestId, accessTokenHash);
    }

    @Override
    public int replaceBookingAccessTokens(Long guestId, String accessTokenHash) {
        return bookingRepository.replaceAccessTokenByGuestId(guestId, accessTokenHash);
    }

    @Override
    public List<Order> findOrdersByAccessToken(String accessTokenHash, int limit) {
        return orderRepository.findByAccessTokenOrderByCreatedAtDescIdDesc(
                accessTokenHash, PageRequest.ofSize(limit));
    }

    @Override
    public List<Order> findOrdersByAccessTokenAfter(
            String accessTokenHash, LocalDateTime createdAt, Long id, int limit) {
        return orderRepository.findByAccessTokenAfterPage(
                accessTokenHash, createdAt, id, PageRequest.ofSize(limit));
    }

    @Override
    public List<Booking> findBookingsByAccessToken(String accessTokenHash, int limit) {
        return bookingRepository.findByAccessTokenWithDetails(
                accessTokenHash, PageRequest.ofSize(limit));
    }

    @Override
    public List<Booking> findBookingsByAccessTokenAfter(
            String accessTokenHash, LocalDateTime createdAt, Long id, int limit) {
        return bookingRepository.findByAccessTokenWithDetailsAfter(
                accessTokenHash, createdAt, id, PageRequest.ofSize(limit));
    }
}
