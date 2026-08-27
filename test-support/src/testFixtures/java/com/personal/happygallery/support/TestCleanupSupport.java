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
import org.springframework.test.jdbc.JdbcTestUtils;

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
        clearReviewData();
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                "admin_mfa_challenge",
                "admin_mfa_recovery_code",
                "admin_auth_history");
        adminUserRepository.deleteAllInBatch();
    }

    public void clearBookingWithPassAndRefundData() {
        clearReviewData();
        clearOrderBenefitData();
        policyConsentRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        bookingCancellationTaskRepository.deleteAllInBatch();
        passLedgerRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
    }

    public void clearBookingReminderData() {
        clearReviewData();
        clearOrderBenefitData();
        policyConsentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        bookingCancellationTaskRepository.deleteAllInBatch();
        passLedgerRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
        notificationLogRepository.deleteAllInBatch();
    }

    public void clearOrderData() {
        clearReviewData();
        clearOrderBenefitData();
        policyConsentRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        orderClaimItemRepository.deleteAllInBatch();
        orderClaimRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        orderApprovalHistoryRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryAdjustmentRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        clearProductOptionData();
        productRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
    }

    public void clearProductData() {
        clearReviewData();
        inventoryAdjustmentRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        clearProductOptionData();
        productRepository.deleteAllInBatch();
    }

    public void clearCartData() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate, "cart_merge_requests", "cart_items");
    }

    private void clearProductOptionData() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                "cart_item_text_inputs",
                "cart_items",
                "product_variant_selections",
                "product_variants",
                "product_option_values",
                "product_option_groups");
    }

    public void clearPassData() {
        clearOrderBenefitData();
        policyConsentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        passLedgerRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
    }

    public void clearBookingData() {
        clearReviewData();
        clearOrderBenefitData();
        policyConsentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        notificationOutboxRepository.deleteAllInBatch();
        bookingCancellationTaskRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
    }

    public void clearUsers() {
        clearReviewData();
        clearOrderBenefitData();
        policyConsentRepository.deleteAllInBatch();
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "cart_merge_requests");
        emailVerificationRepository.deleteAllInBatch();
        socialAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
    }

    public void clearNotificationLogs() {
        notificationOutboxRepository.deleteAllInBatch();
        notificationLogRepository.deleteAllInBatch();
    }

    /** 후기 자식 테이블과 tombstone을 외래 키 역순으로 정리한다. */
    public void clearReviewData() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                "review_images",
                "review_helpful_votes",
                "review_reports",
                "review_moderation_actions",
                "review_evidence_snapshot_images",
                "review_evidence_snapshots",
                "reviews");
    }

    /** 쿠폰·적립금은 주문·결제 시도·회원 모두를 참조하므로 공통 부모보다 먼저 지운다. */
    private void clearOrderBenefitData() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                "reward_ledger",
                "reward_reservation_allocations",
                "reward_reservations",
                "reward_lots",
                "reward_accounts");
        jdbcTemplate.update("""
                UPDATE orders
                SET total_amount = total_amount + coupon_discount_amount,
                    pg_paid_amount = pg_paid_amount + coupon_discount_amount,
                    reward_earn_base = reward_earn_base + coupon_discount_amount,
                    coupon_discount_amount = 0,
                    issued_coupon_id = NULL
                WHERE issued_coupon_id IS NOT NULL
                """);
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate, "issued_coupons", "coupon_definitions");
    }
}
