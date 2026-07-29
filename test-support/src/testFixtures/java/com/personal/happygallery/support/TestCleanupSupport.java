package com.personal.happygallery.support;

import com.personal.happygallery.adapter.out.persistence.admin.AdminUserRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingHistoryRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingCancellationTaskRepository;
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
import com.personal.happygallery.adapter.out.persistence.order.OrderClaimItemRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderClaimRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderItemRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassLedgerRepository;
import com.personal.happygallery.adapter.out.persistence.pass.PassPurchaseRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.adapter.out.persistence.policy.PolicyConsentRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.InventoryAdjustmentRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.user.SocialAccountRepository;
import com.personal.happygallery.adapter.out.persistence.user.EmailVerificationRepository;
import com.personal.happygallery.adapter.out.persistence.user.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TestCleanupSupport {

    private final AdminUserRepository adminUserRepository;
    private final BookingCancellationTaskRepository bookingCancellationTaskRepository;
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
    private final OrderClaimItemRepository orderClaimItemRepository;
    private final OrderClaimRepository orderClaimRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final PassLedgerRepository passLedgerRepository;
    private final PassPurchaseRepository passPurchaseRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PolicyConsentRepository policyConsentRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;
    private final ProductRepository productRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    public TestCleanupSupport(AdminUserRepository adminUserRepository,
                              BookingCancellationTaskRepository bookingCancellationTaskRepository,
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
                              OrderClaimItemRepository orderClaimItemRepository,
                              OrderClaimRepository orderClaimRepository,
                              OrderItemRepository orderItemRepository,
                              OrderRepository orderRepository,
                              PassLedgerRepository passLedgerRepository,
                              PassPurchaseRepository passPurchaseRepository,
                              PaymentAttemptRepository paymentAttemptRepository,
                              PolicyConsentRepository policyConsentRepository,
                              InventoryRepository inventoryRepository,
                              InventoryAdjustmentRepository inventoryAdjustmentRepository,
                              ProductRepository productRepository,
                              SocialAccountRepository socialAccountRepository,
                              EmailVerificationRepository emailVerificationRepository,
                              UserRepository userRepository,
                              JdbcTemplate jdbcTemplate) {
        this.adminUserRepository = adminUserRepository;
        this.bookingCancellationTaskRepository = bookingCancellationTaskRepository;
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
        this.orderClaimItemRepository = orderClaimItemRepository;
        this.orderClaimRepository = orderClaimRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.passLedgerRepository = passLedgerRepository;
        this.passPurchaseRepository = passPurchaseRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.policyConsentRepository = policyConsentRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
        this.productRepository = productRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clearAdminUsers() {
        jdbcTemplate.update("DELETE FROM admin_mfa_challenge");
        jdbcTemplate.update("DELETE FROM admin_mfa_recovery_code");
        jdbcTemplate.update("DELETE FROM admin_auth_history");
        adminUserRepository.deleteAllInBatch();
    }

    public void clearBookingWithPassAndRefundData() {
        policyConsentRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        bookingCancellationTaskRepository.deleteAllInBatch();
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
        policyConsentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        bookingCancellationTaskRepository.deleteAllInBatch();
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
        policyConsentRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        TestDataCleaner.clearOrderData(
                refundRepository,
                orderClaimItemRepository,
                orderClaimRepository,
                fulfillmentRepository,
                orderApprovalHistoryRepository,
                orderItemRepository,
                orderRepository,
                inventoryAdjustmentRepository,
                inventoryRepository,
                productRepository);
        paymentAttemptRepository.deleteAllInBatch();
    }

    public void clearProductData() {
        TestDataCleaner.clearProductData(inventoryAdjustmentRepository, inventoryRepository, productRepository);
    }

    public void clearPassData() {
        policyConsentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        passLedgerRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
    }

    public void clearBookingData() {
        policyConsentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        bookingCancellationTaskRepository.deleteAllInBatch();
        TestDataCleaner.clearBookingData(
                bookingHistoryRepository,
                bookingRepository,
                slotRepository,
                classRepository);
    }

    public void clearUsers() {
        policyConsentRepository.deleteAllInBatch();
        jdbcTemplate.update("DELETE FROM cart_merge_requests");
        emailVerificationRepository.deleteAllInBatch();
        socialAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
    }

    public void clearNotificationLogs() {
        notificationOutboxRepository.deleteAllInBatch();
        notificationLogRepository.deleteAllInBatch();
    }
}
