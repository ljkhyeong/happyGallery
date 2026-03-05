package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.GuestRepository;
import com.personal.happygallery.infra.booking.PhoneVerificationRepository;
import com.personal.happygallery.infra.booking.RefundRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.infra.notification.NotificationLogRepository;
import com.personal.happygallery.infra.pass.PassLedgerRepository;
import com.personal.happygallery.infra.pass.PassPurchaseRepository;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [UseCaseIT] §12.1 8회권 만료 7일 전 알림 발송 검증.
 *
 * <p>Proof (§12.1 DoD): sendExpiryNotifications() 호출 시
 * 만료 7일 내 pass에 PASS_EXPIRY_SOON 알림이 발송되고 notification_log에 기록된다.
 */
@UseCaseIT
class PassExpiryNotificationUseCaseIT {

    @Autowired PassExpiryBatchService passExpiryBatchService;
    @Autowired PassPurchaseRepository passPurchaseRepository;
    @Autowired PassLedgerRepository passLedgerRepository;
    @Autowired RefundRepository refundRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired ClassRepository classRepository;
    @Autowired NotificationLogRepository notificationLogRepository;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // FK 삭제 순서: passLedger → refund → bookingHistory → booking → passPurchase
        //              → phoneVerification → guest → slot → class
        // notification_log FK 없음 → 순서 무관
        passLedgerRepository.deleteAll();
        refundRepository.deleteAll();
        bookingHistoryRepository.deleteAll();
        bookingRepository.deleteAll();
        passPurchaseRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        notificationLogRepository.deleteAll();
        guestRepository.deleteAll();
        slotRepository.deleteAll();
        classRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // Proof: 7일 내 만료 2건 → PASS_EXPIRY_SOON 알림 2건 + notification_log 기록
    // -----------------------------------------------------------------------

    @DisplayName("8회권 만료 알림 배치는 대상 기간 내 8회권에 알림을 발송하고 로그를 남긴다")
    @Test
    void sendExpiryNotifications_withinWindow_sendsAndLogsNotifications() {
        Guest guest1 = guestRepository.save(new Guest("이알림", "01011112222"));
        Guest guest2 = guestRepository.save(new Guest("김알림", "01033334444"));

        // 정확히 7일 후 만료 — 알림 대상
        LocalDateTime soon = LocalDateTime.now(clock).plusDays(7);
        passPurchaseRepository.save(new PassPurchase(guest1, soon, 0L));
        passPurchaseRepository.save(new PassPurchase(guest2, soon, 0L));

        BatchResult result = passExpiryBatchService.sendExpiryNotifications();

        assertThat(result.successCount()).isEqualTo(2);
        assertThat(result.failureCount()).isZero();

        List<NotificationLog> logs = notificationLogRepository.findAll();
        assertThat(logs).hasSize(2);
        assertThat(logs).allMatch(log -> log.getEventType() == NotificationEventType.PASS_EXPIRY_SOON);
    }

    // -----------------------------------------------------------------------
    // Proof: 30일 후 만료 → 알림 없음
    // -----------------------------------------------------------------------

    @DisplayName("8회권 만료 알림 배치는 대상 기간 밖의 8회권을 건너뛴다")
    @Test
    void sendExpiryNotifications_outsideWindow_skips() {
        Guest guest = guestRepository.save(new Guest("박스킵", "01055556666"));

        // 30일 후 만료 — 7일 윈도우 밖
        LocalDateTime later = LocalDateTime.now(clock).plusDays(30);
        passPurchaseRepository.save(new PassPurchase(guest, later, 0L));

        BatchResult result = passExpiryBatchService.sendExpiryNotifications();

        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isZero();
        assertThat(notificationLogRepository.findAll()).isEmpty();
    }

    @DisplayName("8회권 만료 알림 배치를 같은 날 두 번 실행하면 중복 발송을 건너뛴다")
    @Test
    void sendExpiryNotifications_sameDaySecondRun_skipsDuplicates() {
        Guest guest = guestRepository.save(new Guest("중복방지", "01077778888"));
        LocalDateTime target = LocalDateTime.now(clock).plusDays(7);
        passPurchaseRepository.save(new PassPurchase(guest, target, 0L));

        BatchResult first = passExpiryBatchService.sendExpiryNotifications();
        BatchResult second = passExpiryBatchService.sendExpiryNotifications();

        assertThat(first.successCount()).isEqualTo(1);
        assertThat(first.failureCount()).isZero();
        assertThat(second.successCount()).isEqualTo(0);
        assertThat(second.failureCount()).isZero();
        assertThat(notificationLogRepository.findAll()).hasSize(1);
    }
}
