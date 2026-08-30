package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingCancellationTaskRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderClaimRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.adapter.out.persistence.reward.RewardAccountRepository;
import com.personal.happygallery.application.customer.port.out.CustomerAccountActivityPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
class JpaCustomerAccountActivityAdapter implements CustomerAccountActivityPort {

    private final OrderRepository orderRepository;
    private final OrderClaimRepository orderClaimRepository;
    private final BookingRepository bookingRepository;
    private final BookingCancellationTaskRepository bookingCancellationTaskRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final RefundRepository refundRepository;
    private final PaymentAttemptReaderPort paymentAttemptReader;
    private final RewardAccountRepository rewardAccountRepository;

    JpaCustomerAccountActivityAdapter(OrderRepository orderRepository,
                                      OrderClaimRepository orderClaimRepository,
                                      BookingRepository bookingRepository,
                                      BookingCancellationTaskRepository bookingCancellationTaskRepository,
                                      PassPurchaseRepository passPurchaseRepository,
                                      RefundRepository refundRepository,
                                      PaymentAttemptReaderPort paymentAttemptReader,
                                      RewardAccountRepository rewardAccountRepository) {
        this.orderRepository = orderRepository;
        this.orderClaimRepository = orderClaimRepository;
        this.bookingRepository = bookingRepository;
        this.bookingCancellationTaskRepository = bookingCancellationTaskRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.refundRepository = refundRepository;
        this.paymentAttemptReader = paymentAttemptReader;
        this.rewardAccountRepository = rewardAccountRepository;
    }

    @Override
    public boolean hasBlockingActivity(Long userId, LocalDateTime now) {
        return orderRepository.existsUnfinishedByUserId(userId)
                || orderClaimRepository.existsActiveByUserId(userId)
                || bookingRepository.existsBookedByUserId(userId)
                || bookingCancellationTaskRepository.existsPendingByUserId(userId)
                || passPurchaseRepository.existsUsableByUserId(userId, now)
                || refundRepository.existsUnresolvedByUserId(userId)
                || paymentAttemptReader.existsNonTerminalByOwnerUserId(userId)
                || rewardAccountRepository.existsBlockingWithdrawal(userId);
    }
}
