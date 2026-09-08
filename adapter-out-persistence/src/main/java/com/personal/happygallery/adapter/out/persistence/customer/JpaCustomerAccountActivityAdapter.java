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
import java.util.ArrayList;
import java.util.List;
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
    public List<BlockingActivity> findBlockingActivities(Long userId, LocalDateTime now) {
        List<BlockingActivity> activities = new ArrayList<>();
        if (orderRepository.existsUnfinishedByUserId(userId)) activities.add(BlockingActivity.ORDER);
        if (orderClaimRepository.existsActiveByUserId(userId)) activities.add(BlockingActivity.CLAIM);
        if (bookingRepository.existsBookedByUserId(userId)) activities.add(BlockingActivity.BOOKING);
        if (bookingCancellationTaskRepository.existsPendingByUserId(userId)) activities.add(BlockingActivity.CANCELLATION_TASK);
        if (passPurchaseRepository.existsUsableByUserId(userId, now)) activities.add(BlockingActivity.PASS);
        if (refundRepository.existsUnresolvedByUserId(userId)) activities.add(BlockingActivity.REFUND);
        if (paymentAttemptReader.existsNonTerminalByOwnerUserId(userId)) activities.add(BlockingActivity.PAYMENT);
        if (rewardAccountRepository.existsBlockingWithdrawal(userId)) activities.add(BlockingActivity.REWARD);
        return List.copyOf(activities);
    }
}
