package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.booking.port.in.PublicHolidaySyncUseCase;
import com.personal.happygallery.application.order.port.in.OrderAutoRefundBatchUseCase;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingRegistrationUseCase;
import com.personal.happygallery.application.order.port.in.SmartStoreOrderSyncBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentAttemptExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentWebhookBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentSettlementSyncUseCase;
import com.personal.happygallery.application.payment.port.in.RefundRecoveryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreStockSyncBatchUseCase;
import com.personal.happygallery.domain.time.Clocks;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 운영 배치 스케줄러 (§10.2).
 *
 * <p>스케줄 타이밍을 한 곳에서 관리하고, 실제 로직은 각 서비스에 위임한다.
 * 이 클래스는 {@code @Transactional}을 갖지 않으며, 트랜잭션은 각 서비스 메서드에서 처리한다.
 *
 * <ul>
 *   <li>매시간 정각: 주문 승인 SLA 초과 자동환불, 픽업 만료 처리, 픽업 마감 2시간 전 알림</li>
 *   <li>매일 00:00: 8회권 크레딧 소멸</li>
 *   <li>매시간: 예약 D-1·당일 및 8회권 만료 7일 전 알림 catch-up</li>
 *   <li>매분 5초: 시작하지 않은 결제 준비 만료</li>
 *   <li>매분 15초: 실행되지 않았거나 결과 확인이 필요한 환불 복구</li>
 *   <li>매분 25초: 외부 배송조회 서비스 운송장 등록</li>
 *   <li>매분 35초: 결제 상태 웹훅 처리</li>
 *   <li>매분 45초: confirm 도중 중단된 결제 확정 복구</li>
 *   <li>매분 50초: 스마트스토어 변경 주문 수집과 내부 재고 반영</li>
 *   <li>매분 55초: 내부 최신 재고를 스마트스토어에 반영</li>
 *   <li>매월 1일 04:20: 공식 공휴일 연도별 스냅샷 동기화</li>
 *   <li>매일 03:30: 보존 기간이 지난 결제·휴대폰 인증 개인정보 정리</li>
 * </ul>
 */
@Component
public class BatchScheduler {

    private final OrderAutoRefundBatchUseCase orderAutoRefundBatchUseCase;
    private final PickupExpireBatchUseCase pickupExpireBatchUseCase;
    private final PickupDeadlineReminderBatchUseCase pickupDeadlineReminderBatchUseCase;
    private final PassExpiryBatchUseCase passExpiryBatchUseCase;
    private final BookingReminderBatchUseCase bookingReminderBatchUseCase;
    private final RefundRecoveryUseCase refundRecoveryUseCase;
    private final PaymentConfirmRecoveryUseCase paymentConfirmRecoveryUseCase;
    private final PaymentAttemptExpiryBatchUseCase paymentAttemptExpiryBatchUseCase;
    private final PersonalDataRetentionBatchUseCase personalDataRetentionBatchUseCase;
    private final ShipmentTrackingRegistrationUseCase shipmentTrackingRegistrationUseCase;
    private final PaymentWebhookBatchUseCase paymentWebhookBatchUseCase;
    private final PublicHolidaySyncUseCase publicHolidaySyncUseCase;
    private final SmartStoreStockSyncBatchUseCase smartStoreStockSyncBatchUseCase;
    private final SmartStoreOrderSyncBatchUseCase smartStoreOrderSyncBatchUseCase;
    private final PaymentSettlementSyncUseCase paymentSettlementSyncUseCase;

    public BatchScheduler(OrderAutoRefundBatchUseCase orderAutoRefundBatchUseCase,
                          PickupExpireBatchUseCase pickupExpireBatchUseCase,
                          PickupDeadlineReminderBatchUseCase pickupDeadlineReminderBatchUseCase,
                          PassExpiryBatchUseCase passExpiryBatchUseCase,
                          BookingReminderBatchUseCase bookingReminderBatchUseCase,
                          RefundRecoveryUseCase refundRecoveryUseCase,
                          PaymentConfirmRecoveryUseCase paymentConfirmRecoveryUseCase,
                          PaymentAttemptExpiryBatchUseCase paymentAttemptExpiryBatchUseCase,
                          PersonalDataRetentionBatchUseCase personalDataRetentionBatchUseCase,
                          ShipmentTrackingRegistrationUseCase shipmentTrackingRegistrationUseCase,
                          PaymentWebhookBatchUseCase paymentWebhookBatchUseCase,
                          PublicHolidaySyncUseCase publicHolidaySyncUseCase,
                          SmartStoreStockSyncBatchUseCase smartStoreStockSyncBatchUseCase,
                          SmartStoreOrderSyncBatchUseCase smartStoreOrderSyncBatchUseCase,
                          PaymentSettlementSyncUseCase paymentSettlementSyncUseCase) {
        this.orderAutoRefundBatchUseCase = orderAutoRefundBatchUseCase;
        this.pickupExpireBatchUseCase = pickupExpireBatchUseCase;
        this.pickupDeadlineReminderBatchUseCase = pickupDeadlineReminderBatchUseCase;
        this.passExpiryBatchUseCase = passExpiryBatchUseCase;
        this.bookingReminderBatchUseCase = bookingReminderBatchUseCase;
        this.refundRecoveryUseCase = refundRecoveryUseCase;
        this.paymentConfirmRecoveryUseCase = paymentConfirmRecoveryUseCase;
        this.paymentAttemptExpiryBatchUseCase = paymentAttemptExpiryBatchUseCase;
        this.personalDataRetentionBatchUseCase = personalDataRetentionBatchUseCase;
        this.shipmentTrackingRegistrationUseCase = shipmentTrackingRegistrationUseCase;
        this.paymentWebhookBatchUseCase = paymentWebhookBatchUseCase;
        this.publicHolidaySyncUseCase = publicHolidaySyncUseCase;
        this.smartStoreStockSyncBatchUseCase = smartStoreStockSyncBatchUseCase;
        this.smartStoreOrderSyncBatchUseCase = smartStoreOrderSyncBatchUseCase;
        this.paymentSettlementSyncUseCase = paymentSettlementSyncUseCase;
    }

    /** 주문 승인 SLA(24h) 초과 → 자동환불. 매시간 정각 실행. */
    @BatchJob(id = "order_auto_refund", value = "주문 자동환불")
    @Scheduled(cron = "0 0 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runOrderAutoRefund() {
        return orderAutoRefundBatchUseCase.autoRefundExpired();
    }

    /** 픽업 마감 초과 → 기성품 재고 복구 후 미환불 종료, 주문제작 미환불 종료. 매시간 정각 실행. */
    @BatchJob(id = "pickup_expire", value = "픽업 만료")
    @Scheduled(cron = "0 0 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPickupExpire() {
        return pickupExpireBatchUseCase.expirePickups();
    }

    /** 만료된 8회권 크레딧 소멸. 매일 00:00 실행. */
    @BatchJob(id = "pass_expiry", value = "8회권 크레딧 소멸")
    @Scheduled(cron = "0 0 0 * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPassExpiry() {
        return passExpiryBatchUseCase.expireAll();
    }

    /** 8회권 만료 7일 전 알림. 중단 뒤 보충할 수 있도록 매시간 15분 실행. */
    @BatchJob(id = "pass_expiry_notification", value = "8회권 만료 7일 전 알림")
    @Scheduled(cron = "0 15 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPassExpiryNotification() {
        return passExpiryBatchUseCase.sendExpiryNotifications();
    }

    /** 픽업 마감 2시간 전 알림. 매시간 정각 실행. */
    @BatchJob(id = "pickup_deadline_reminder", value = "픽업 마감 알림")
    @Scheduled(cron = "0 0 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPickupDeadlineReminder() {
        return pickupDeadlineReminderBatchUseCase.sendPickupDeadlineReminders();
    }

    /** 예약 D-1 리마인드. 중단 뒤 보충할 수 있도록 매시간 5분 실행. */
    @BatchJob(id = "booking_d1_reminder", value = "D-1 예약 리마인드")
    @Scheduled(cron = "0 5 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runBookingD1Reminder() {
        return bookingReminderBatchUseCase.sendD1Reminders();
    }

    /** 예약 당일 리마인드. 07:00 이후 보충할 수 있도록 매시간 10분 실행. */
    @BatchJob(id = "booking_same_day_reminder", value = "당일 예약 리마인드")
    @Scheduled(cron = "0 10 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runBookingSameDayReminder() {
        return bookingReminderBatchUseCase.sendSameDayReminders();
    }

    /** 유실된 요청과 오래된 처리 중 환불을 같은 멱등키로 복구한다. 매분 15초에 실행. */
    @BatchJob(id = "refund_recovery", value = "환불 복구")
    @Scheduled(cron = "15 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runRefundRecovery() {
        return refundRecoveryUseCase.recoverPendingRefunds();
    }

    /** confirm 도중 중단된 결제를 같은 멱등키로 재개한다. 매분 45초에 실행. */
    @BatchJob(id = "payment_confirm_recovery", value = "결제 확정 복구")
    @Scheduled(cron = "45 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPaymentConfirmRecovery() {
        return paymentConfirmRecoveryUseCase.recoverIncompleteConfirms();
    }

    /** confirm을 시작하지 않고 30분이 지난 결제 준비를 만료 처리한다. 매분 5초에 실행. */
    @BatchJob(id = "payment_attempt_expiry", value = "결제 준비 만료")
    @Scheduled(cron = "5 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPaymentAttemptExpiry() {
        return paymentAttemptExpiryBatchUseCase.expirePendingAttempts();
    }

    /** 외부 배송조회 서비스에 미등록 운송장을 등록한다. 매분 25초에 실행. */
    @BatchJob(id = "shipment_tracking_registration", value = "배송조회 등록")
    @Scheduled(cron = "25 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runShipmentTrackingRegistration() {
        return shipmentTrackingRegistrationUseCase.registerPendingShipments();
    }

    /** 스마트스토어 변경 주문을 가져와 내부 재고에 반영한다. 매분 50초에 실행. */
    @BatchJob(id = "smartstore_order_sync", value = "스마트스토어 주문 동기화")
    @Scheduled(cron = "50 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runSmartStoreOrderSync() {
        return smartStoreOrderSyncBatchUseCase.syncChangedOrders();
    }

    /** 해피갤러리의 최신 재고를 스마트스토어에 반영한다. 매분 55초에 실행. */
    @BatchJob(id = "smartstore_stock_sync", value = "스마트스토어 재고 동기화")
    @Scheduled(cron = "55 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runSmartStoreStockSync() {
        return smartStoreStockSyncBatchUseCase.syncPendingStocks();
    }

    /** 최근 7일 Toss 정산 내역을 로컬 결제·환불 원장과 비교한다. 매시간 40분 실행. */
    @BatchJob(id = "payment_settlement_sync", value = "결제 정산 대사")
    @Scheduled(cron = "0 40 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPaymentSettlementSync() {
        return paymentSettlementSyncUseCase.synchronizeRecent();
    }

    /** Toss 결제 상태 변경 웹훅을 기존 PG 대사 흐름으로 처리한다. 매분 35초에 실행. */
    @BatchJob(id = "payment_webhook_receipt", value = "결제 웹훅 처리")
    @Scheduled(cron = "35 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPaymentWebhookReceipts() {
        return paymentWebhookBatchUseCase.processPendingReceipts();
    }

    /** 공식 공휴일 연도별 스냅샷을 갱신한다. 매월 1일 04:20 실행. */
    @BatchJob(id = "public_holiday_snapshot", value = "공식 공휴일 동기화")
    @Scheduled(cron = "0 20 4 1 * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPublicHolidaySnapshotSync() {
        return publicHolidaySyncUseCase.syncAnnualSnapshots();
    }

    /** 보존 기간이 지난 결제·휴대폰 인증·장바구니 병합 기록을 정리한다. 매일 03:30 실행. */
    @BatchJob(id = "personal_data_retention", value = "개인정보 보존 기간 정리")
    @Scheduled(cron = "0 30 3 * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPersonalDataRetention() {
        return personalDataRetentionBatchUseCase.cleanUpExpiredSensitiveData();
    }
}
