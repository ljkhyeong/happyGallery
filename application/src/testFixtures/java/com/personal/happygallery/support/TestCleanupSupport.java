package com.personal.happygallery.support;

import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.GuestRepository;
import com.personal.happygallery.adapter.out.persistence.booking.PhoneVerificationRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationLogRepository;
import com.personal.happygallery.adapter.out.persistence.order.FulfillmentRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderItemRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassLedgerRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class TestCleanupSupport {

    private final BookingHistoryRepository bookingHistoryRepository;
    private final BookingRepository bookingRepository;
    private final ClassRepository classRepository;
    private final GuestRepository guestRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final RefundRepository refundRepository;
    private final SlotRepository slotRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public TestCleanupSupport(BookingHistoryRepository bookingHistoryRepository,
                              BookingRepository bookingRepository,
                              ClassRepository classRepository,
                              GuestRepository guestRepository,
                              PhoneVerificationRepository phoneVerificationRepository,
                              RefundRepository refundRepository,
                              SlotRepository slotRepository,
                              NotificationLogRepository notificationLogRepository,
                              FulfillmentRepository fulfillmentRepository,
                              OrderApprovalHistoryRepository orderApprovalHistoryRepository,
                              OrderItemRepository orderItemRepository,
                              OrderRepository orderRepository,
                              PassLedgerRepository passLedgerRepository,
                              PassPurchaseRepository passPurchaseRepository,
                              PaymentAttemptRepository paymentAttemptRepository,
                              InventoryRepository inventoryRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository) {
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.bookingRepository = bookingRepository;
        this.classRepository = classRepository;
        this.guestRepository = guestRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.refundRepository = refundRepository;
        this.slotRepository = slotRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public void clearBookingWithPassAndRefundData() {
        paymentAttemptRepository.deleteAllInBatch();
        TestDataCleaner.clearBookingWithPassAndRefundData(
                passLedgerRepository,
                refundRepository,
                bookingHistoryRepository,
                bookingRepository,
                passPurchaseRepository,
                phoneVerificationRepository,
                guestRepository,
                slotRepository,
                classRepository);
    }

    public void clearBookingReminderData() {
        paymentAttemptRepository.deleteAllInBatch();
        TestDataCleaner.clearBookingReminderData(
                passLedgerRepository,
                passPurchaseRepository,
                bookingHistoryRepository,
                bookingRepository,
                guestRepository,
                slotRepository,
                classRepository,
                notificationLogRepository);
    }

    public void clearOrderData() {
        paymentAttemptRepository.deleteAllInBatch();
        TestDataCleaner.clearOrderData(
                refundRepository,
                fulfillmentRepository,
                orderApprovalHistoryRepository,
                orderItemRepository,
                orderRepository,
                inventoryRepository,
                productRepository);
    }

    public void clearPassData() {
        paymentAttemptRepository.deleteAllInBatch();
        passLedgerRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
    }

    public void clearBookingData() {
        paymentAttemptRepository.deleteAllInBatch();
        TestDataCleaner.clearBookingData(
                bookingHistoryRepository,
                bookingRepository,
                slotRepository,
                classRepository);
    }

    public void clearUsers() {
        userRepository.deleteAllInBatch();
    }

    public void clearNotificationLogs() {
        notificationLogRepository.deleteAllInBatch();
    }
}
