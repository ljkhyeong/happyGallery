package com.personal.happygallery.app.pass;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.customer.port.out.UserStorePort;
import com.personal.happygallery.app.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.app.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [UseCaseIT] §12.1 8회권 만료 7일 전 알림 발송 검증.
 *
 * <p>Proof (§12.1 DoD): sendExpiryNotifications() 호출 시
 * 만료 7일 내 pass에 PASS_EXPIRY_SOON 알림이 발송되고 notification_log에 기록된다.
 */
@UseCaseIT
class PassExpiryNotificationUseCaseIT {

    @Autowired PassExpiryBatchUseCase passExpiryBatchService;
    @Autowired UserStorePort userStorePort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
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
        cleanupSupport.clearBookingWithPassAndRefundData();
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearUsers();
    }

    // -----------------------------------------------------------------------
    // Proof: 7일 내 만료 2건 → PASS_EXPIRY_SOON 알림 2건 + notification_log 기록
    // -----------------------------------------------------------------------

    @DisplayName("8회권 만료 알림 배치는 대상 기간 내 8회권에 알림을 발송하고 로그를 남긴다")
    @Test
    void sendExpiryNotifications_withinWindow_sendsAndLogsNotifications() {
        User user1 = userStorePort.save(new User("pass-expiry-1@example.com", "hashed-password", "회원", "01011112222"));
        User user2 = userStorePort.save(new User("pass-expiry-2@example.com", "hashed-password", "회원", "01033334444"));

        // 정확히 7일 후 만료 — 알림 대상
        LocalDateTime soon = LocalDateTime.now(clock).plusDays(7);
        passPurchaseStorePort.save(passPurchase(user1.getId(), soon, 0L));
        passPurchaseStorePort.save(passPurchase(user2.getId(), soon, 0L));

        BatchResult result = passExpiryBatchService.sendExpiryNotifications();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 2);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(2);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(logs).hasSize(2);
            softly.assertThat(logs).allMatch(log -> log.getEventType() == NotificationEventType.PASS_EXPIRY_SOON);
            softly.assertThat(logs).extracting(NotificationLog::getUserId).containsExactlyInAnyOrder(user1.getId(), user2.getId());
        });
    }

    // -----------------------------------------------------------------------
    // Proof: 30일 후 만료 → 알림 없음
    // -----------------------------------------------------------------------

    @DisplayName("8회권 만료 알림 배치는 대상 기간 밖의 8회권을 건너뛴다")
    @Test
    void sendExpiryNotifications_outsideWindow_skips() {
        User user = userStorePort.save(new User("pass-expiry-skip@example.com", "hashed-password", "회원", "01055556666"));

        // 30일 후 만료 — 7일 윈도우 밖
        LocalDateTime later = LocalDateTime.now(clock).plusDays(30);
        passPurchaseStorePort.save(passPurchase(user.getId(), later, 0L));

        BatchResult result = passExpiryBatchService.sendExpiryNotifications();

        assertThat(result.successCount()).isEqualTo(0);
        assertThat(result.failureCount()).isZero();
        assertThat(notificationLogProbe.all()).isEmpty();
    }

    @DisplayName("8회권 만료 알림 배치를 같은 날 두 번 실행하면 중복 발송을 건너뛴다")
    @Test
    void sendExpiryNotifications_sameDaySecondRun_skipsDuplicates() {
        User user = userStorePort.save(new User("pass-expiry-dedupe@example.com", "hashed-password", "회원", "01077778888"));
        LocalDateTime target = LocalDateTime.now(clock).plusDays(7);
        passPurchaseStorePort.save(passPurchase(user.getId(), target, 0L));

        BatchResult first = passExpiryBatchService.sendExpiryNotifications();
        awaitLogCount(notificationLogProbe, 1);
        BatchResult second = passExpiryBatchService.sendExpiryNotifications();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);

        assertSoftly(softly -> {
            softly.assertThat(first.successCount()).isEqualTo(1);
            softly.assertThat(first.failureCount()).isZero();
            softly.assertThat(second.successCount()).isEqualTo(0);
            softly.assertThat(second.failureCount()).isZero();
            softly.assertThat(logs).hasSize(1);
        });
    }
}
