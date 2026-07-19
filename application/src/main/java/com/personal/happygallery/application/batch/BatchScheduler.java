package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.booking.port.in.BookingReminderBatchUseCase;
import com.personal.happygallery.application.order.port.in.OrderAutoRefundBatchUseCase;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.in.PickupExpireBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.RefundRecoveryUseCase;
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
 *   <li>매일 00:00: 8회권 크레딧 소멸, 예약 D-1 리마인드</li>
 *   <li>매일 07:00: 예약 당일 리마인드</li>
 *   <li>매일 09:00: 8회권 만료 7일 전 알림</li>
 *   <li>매분 15초: 실행되지 않았거나 결과 확인이 필요한 환불 복구</li>
 *   <li>매분 45초: confirm 도중 중단된 결제 확정 복구</li>
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

    public BatchScheduler(OrderAutoRefundBatchUseCase orderAutoRefundBatchUseCase,
                          PickupExpireBatchUseCase pickupExpireBatchUseCase,
                          PickupDeadlineReminderBatchUseCase pickupDeadlineReminderBatchUseCase,
                          PassExpiryBatchUseCase passExpiryBatchUseCase,
                          BookingReminderBatchUseCase bookingReminderBatchUseCase,
                          RefundRecoveryUseCase refundRecoveryUseCase,
                          PaymentConfirmRecoveryUseCase paymentConfirmRecoveryUseCase) {
        this.orderAutoRefundBatchUseCase = orderAutoRefundBatchUseCase;
        this.pickupExpireBatchUseCase = pickupExpireBatchUseCase;
        this.pickupDeadlineReminderBatchUseCase = pickupDeadlineReminderBatchUseCase;
        this.passExpiryBatchUseCase = passExpiryBatchUseCase;
        this.bookingReminderBatchUseCase = bookingReminderBatchUseCase;
        this.refundRecoveryUseCase = refundRecoveryUseCase;
        this.paymentConfirmRecoveryUseCase = paymentConfirmRecoveryUseCase;
    }

    /** 주문 승인 SLA(24h) 초과 → 자동환불. 매시간 정각 실행. */
    @BatchJob("주문 자동환불")
    @Scheduled(cron = "0 0 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runOrderAutoRefund() {
        return orderAutoRefundBatchUseCase.autoRefundExpired();
    }

    /** 픽업 마감 초과 → 기성품 환불, 주문제작 미환불 만료. 매시간 정각 실행. */
    @BatchJob("픽업 만료")
    @Scheduled(cron = "0 0 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPickupExpire() {
        return pickupExpireBatchUseCase.expirePickups();
    }

    /** 만료된 8회권 크레딧 소멸. 매일 00:00 실행. */
    @BatchJob("8회권 크레딧 소멸")
    @Scheduled(cron = "0 0 0 * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPassExpiry() {
        return passExpiryBatchUseCase.expireAll();
    }

    /** 8회권 만료 7일 전 알림. 매일 09:00 실행. */
    @BatchJob("8회권 만료 7일 전 알림")
    @Scheduled(cron = "0 0 9 * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPassExpiryNotification() {
        return passExpiryBatchUseCase.sendExpiryNotifications();
    }

    /** 픽업 마감 2시간 전 알림. 매시간 정각 실행. */
    @BatchJob("픽업 마감 알림")
    @Scheduled(cron = "0 0 * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPickupDeadlineReminder() {
        return pickupDeadlineReminderBatchUseCase.sendPickupDeadlineReminders();
    }

    /** 예약 D-1 리마인드. 매일 00:00 실행. */
    @BatchJob("D-1 예약 리마인드")
    @Scheduled(cron = "0 0 0 * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runBookingD1Reminder() {
        return bookingReminderBatchUseCase.sendD1Reminders();
    }

    /** 예약 당일 리마인드. 매일 07:00 실행. */
    @BatchJob("당일 예약 리마인드")
    @Scheduled(cron = "0 0 7 * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runBookingSameDayReminder() {
        return bookingReminderBatchUseCase.sendSameDayReminders();
    }

    /** 유실된 요청과 오래된 처리 중 환불을 같은 멱등키로 복구한다. 매분 15초에 실행. */
    @BatchJob("환불 복구")
    @Scheduled(cron = "15 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runRefundRecovery() {
        return refundRecoveryUseCase.recoverPendingRefunds();
    }

    /** confirm 도중 중단된 결제를 같은 멱등키로 재개한다. 매분 45초에 실행. */
    @BatchJob("결제 확정 복구")
    @Scheduled(cron = "45 * * * * *", zone = Clocks.SEOUL_ID)
    public BatchResult runPaymentConfirmRecovery() {
        return paymentConfirmRecoveryUseCase.recoverIncompleteConfirms();
    }
}
