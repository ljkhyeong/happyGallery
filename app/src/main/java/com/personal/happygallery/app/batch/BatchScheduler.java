package com.personal.happygallery.app.batch;

import com.personal.happygallery.app.booking.BookingReminderBatchService;
import com.personal.happygallery.app.order.OrderAutoRefundBatchService;
import com.personal.happygallery.app.order.PickupExpireBatchService;
import com.personal.happygallery.app.pass.PassExpiryBatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 운영 배치 스케줄러 (§10.2).
 *
 * <p>스케줄 타이밍을 한 곳에서 관리하고, 실제 로직은 각 서비스에 위임한다.
 * 이 클래스는 {@code @Transactional}을 갖지 않으며, 트랜잭션은 각 서비스 메서드에서 처리한다.
 *
 * <ul>
 *   <li>매시간 정각: 주문 승인 SLA 초과 자동환불, 픽업 마감 자동취소</li>
 *   <li>매일 00:00: 8회권 크레딧 소멸, 예약 D-1 리마인드</li>
 *   <li>매일 07:00: 예약 당일 리마인드</li>
 *   <li>매일 09:00: 8회권 만료 7일 전 알림</li>
 * </ul>
 */
@Component
public class BatchScheduler {

    private final OrderAutoRefundBatchService orderAutoRefundBatchService;
    private final PickupExpireBatchService pickupExpireBatchService;
    private final PassExpiryBatchService passExpiryBatchService;
    private final BookingReminderBatchService bookingReminderBatchService;

    public BatchScheduler(OrderAutoRefundBatchService orderAutoRefundBatchService,
                          PickupExpireBatchService pickupExpireBatchService,
                          PassExpiryBatchService passExpiryBatchService,
                          BookingReminderBatchService bookingReminderBatchService) {
        this.orderAutoRefundBatchService = orderAutoRefundBatchService;
        this.pickupExpireBatchService = pickupExpireBatchService;
        this.passExpiryBatchService = passExpiryBatchService;
        this.bookingReminderBatchService = bookingReminderBatchService;
    }

    /** 주문 승인 SLA(24h) 초과 → 자동환불. 매시간 정각 실행. */
    @BatchJob("주문 자동환불")
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public int runOrderAutoRefund() {
        return orderAutoRefundBatchService.autoRefundExpired();
    }

    /** 픽업 마감 초과 → 자동취소·환불. 매시간 정각 실행. */
    @BatchJob("픽업 만료")
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public int runPickupExpire() {
        return pickupExpireBatchService.expirePickups();
    }

    /** 만료된 8회권 크레딧 소멸. 매일 00:00 실행. */
    @BatchJob("8회권 크레딧 소멸")
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public int runPassExpiry() {
        return passExpiryBatchService.expireAll();
    }

    /** 8회권 만료 7일 전 알림. 매일 09:00 실행. */
    @BatchJob("8회권 만료 7일 전 알림")
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public int runPassExpiryNotification() {
        return passExpiryBatchService.sendExpiryNotifications();
    }

    /** 예약 D-1 리마인드. 매일 00:00 실행. */
    @BatchJob("D-1 예약 리마인드")
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public int runBookingD1Reminder() {
        return bookingReminderBatchService.sendD1Reminders();
    }

    /** 예약 당일 리마인드. 매일 07:00 실행. */
    @BatchJob("당일 예약 리마인드")
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
    public int runBookingSameDayReminder() {
        return bookingReminderBatchService.sendSameDayReminders();
    }
}
