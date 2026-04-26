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
import com.personal.happygallery.adapter.out.persistence.product.InventoryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;

/**
 * 통합 테스트 데이터 정리 유틸.
 *
 * <p>FK 순서를 맞춰 deleteAllInBatch()를 호출해 중복 코드를 줄인다.
 */
public final class TestDataCleaner {

    private TestDataCleaner() {
    }

    public static void clearBookingData(BookingHistoryRepository bookingHistoryRepository,
                                        BookingRepository bookingRepository,
                                        SlotRepository slotRepository,
                                        ClassRepository classRepository) {
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
    }

    public static void clearBookingWithPassData(PassLedgerRepository passLedgerRepository,
                                                BookingHistoryRepository bookingHistoryRepository,
                                                BookingRepository bookingRepository,
                                                PassPurchaseRepository passPurchaseRepository,
                                                PhoneVerificationRepository phoneVerificationRepository,
                                                GuestRepository guestRepository,
                                                SlotRepository slotRepository,
                                                ClassRepository classRepository) {
        passLedgerRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
    }

    public static void clearBookingWithPassAndRefundData(PassLedgerRepository passLedgerRepository,
                                                         RefundRepository refundRepository,
                                                         BookingHistoryRepository bookingHistoryRepository,
                                                         BookingRepository bookingRepository,
                                                         PassPurchaseRepository passPurchaseRepository,
                                                         PhoneVerificationRepository phoneVerificationRepository,
                                                         GuestRepository guestRepository,
                                                         SlotRepository slotRepository,
                                                         ClassRepository classRepository) {
        passLedgerRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
    }

    public static void clearBookingReminderData(PassLedgerRepository passLedgerRepository,
                                                PassPurchaseRepository passPurchaseRepository,
                                                BookingHistoryRepository bookingHistoryRepository,
                                                BookingRepository bookingRepository,
                                                GuestRepository guestRepository,
                                                SlotRepository slotRepository,
                                                ClassRepository classRepository,
                                                NotificationLogRepository notificationLogRepository) {
        passLedgerRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
        notificationLogRepository.deleteAllInBatch();
    }

    public static void clearPassNotificationData(PassLedgerRepository passLedgerRepository,
                                                 RefundRepository refundRepository,
                                                 BookingHistoryRepository bookingHistoryRepository,
                                                 BookingRepository bookingRepository,
                                                 PassPurchaseRepository passPurchaseRepository,
                                                 PhoneVerificationRepository phoneVerificationRepository,
                                                 NotificationLogRepository notificationLogRepository,
                                                 GuestRepository guestRepository,
                                                 SlotRepository slotRepository,
                                                 ClassRepository classRepository) {
        passLedgerRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        bookingHistoryRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        phoneVerificationRepository.deleteAllInBatch();
        notificationLogRepository.deleteAllInBatch();
        passPurchaseRepository.deleteAllInBatch();
        guestRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        classRepository.deleteAllInBatch();
    }

    public static void clearOrderData(RefundRepository refundRepository,
                                      FulfillmentRepository fulfillmentRepository,
                                      OrderApprovalHistoryRepository orderApprovalHistoryRepository,
                                      OrderItemRepository orderItemRepository,
                                      OrderRepository orderRepository,
                                      InventoryRepository inventoryRepository,
                                      ProductRepository productRepository) {
        refundRepository.deleteAllInBatch();
        fulfillmentRepository.deleteAllInBatch();
        orderApprovalHistoryRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }

    public static void clearProductData(InventoryRepository inventoryRepository,
                                        ProductRepository productRepository) {
        inventoryRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
    }
}
