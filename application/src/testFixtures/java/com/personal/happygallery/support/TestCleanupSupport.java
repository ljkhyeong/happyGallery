package com.personal.happygallery.support;

import com.personal.happygallery.adapter.out.persistence.admin.AdminUserRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.GuestRepository;
import com.personal.happygallery.adapter.out.persistence.booking.PhoneVerificationRepository;
import com.personal.happygallery.adapter.out.persistence.booking.RefundRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationLogRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.adapter.out.persistence.order.FulfillmentRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderApprovalHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderItemRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassLedgerRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.adapter.out.persistence.user.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class TestCleanupSupport {

    private final AdminUserRepository adminUserRepository;
    private final BookingHistoryRepository bookingHistoryRepository;
    private final BookingRepository bookingRepository;
    private final ClassRepository classRepository;
    private final GuestRepository guestRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final RefundRepository refundRepository;
    private final SlotRepository slotRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final OrderApprovalHistoryRepository orderApprovalHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;

    public TestCleanupSupport(AdminUserRepository adminUserRepository,
                              BookingHistoryRepository bookingHistoryRepository,
                              BookingRepository bookingRepository,
                              ClassRepository classRepository,
                              GuestRepository guestRepository,
                              PhoneVerificationRepository phoneVerificationRepository,
                              RefundRepository refundRepository,
                              SlotRepository slotRepository,
                              NotificationLogRepository notificationLogRepository,
                              NotificationOutboxRepository notificationOutboxRepository,
                              FulfillmentRepository fulfillmentRepository,
                              OrderApprovalHistoryRepository orderApprovalHistoryRepository,
                              OrderItemRepository orderItemRepository,
                              OrderRepository orderRepository,
                              PassLedgerRepository passLedgerRepository,
                              PassPurchaseRepository passPurchaseRepository,
                              PaymentAttemptRepository paymentAttemptRepository,
                              InventoryRepository inventoryRepository,
                              ProductRepository productRepository,
                              SocialAccountRepository socialAccountRepository,
                              UserRepository userRepository) {
        this.adminUserRepository = adminUserRepository;
        this.bookingHistoryRepository = bookingHistoryRepository;
        this.bookingRepository = bookingRepository;
        this.classRepository = classRepository;
        this.guestRepository = guestRepository;
        this.phoneVerificationRepository = phoneVerificationRepository;
        this.refundRepository = refundRepository;
        this.slotRepository = slotRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.orderApprovalHistoryRepository = orderApprovalHistoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
    }

    public void clearAdminUsers() {
        adminUserRepository.deleteAllInBatch();
    }

    public void clearBookingWithPassAndRefundData() {
        notificationOutboxRepository.deleteAllInBatch();
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
        paymentAttemptRepository.deleteAllInBatch();
    }

    public void clearBookingReminderData() {
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
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
        notificationOutboxRepository.deleteAllInBatch();
        TestDataCleaner.clearOrderData(
                refundRepository,
                fulfillmentRepository,
                orderApprovalHistoryRepository,
                orderItemRepository,
                orderRepository,
                inventoryRepository,
                productRepository);
        paymentAttemptRepository.deleteAllInBatch();
    }

    public void clearProductData() {
        TestDataCleaner.clearProductData(inventoryRepository, productRepository);
    }

    public void clearPassData() {
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        passLedgerRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
    }

    public void clearBookingData() {
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        TestDataCleaner.clearBookingData(
                bookingHistoryRepository,
                bookingRepository,
                slotRepository,
                classRepository);
    }

    public void clearUsers() {
        socialAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
    }

    public void clearNotificationLogs() {
        notificationOutboxRepository.deleteAllInBatch();
        notificationLogRepository.deleteAllInBatch();
    }
}
