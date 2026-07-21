package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.application.customer.port.out.CustomerAccountActivityPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
class JpaCustomerAccountActivityAdapter implements CustomerAccountActivityPort {

    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final RefundRepository refundRepository;

    JpaCustomerAccountActivityAdapter(OrderRepository orderRepository,
                                      BookingRepository bookingRepository,
                                      PassPurchaseRepository passPurchaseRepository,
                                      RefundRepository refundRepository) {
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.refundRepository = refundRepository;
    }

    @Override
    public boolean hasBlockingActivity(Long userId, LocalDateTime now) {
        return orderRepository.existsUnfinishedByUserId(userId)
                || bookingRepository.existsBookedByUserId(userId)
                || passPurchaseRepository.existsUsableByUserId(userId, now)
                || refundRepository.existsUnresolvedByUserId(userId);
    }
}
