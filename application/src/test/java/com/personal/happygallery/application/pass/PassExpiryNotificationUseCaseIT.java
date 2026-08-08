package com.personal.happygallery.application.pass;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.NotificationOutboxDispatcher;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxInsertPort;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.out.PassPurchaseStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.passPurchase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

/**
 * [UseCaseIT] §12.1 8회권 만료 7일 전 알림 발송 검증.
 *
 * <p>Proof (§12.1 DoD): sendExpiryNotifications() 호출 시
 * 만료 7일 내 pass에 PASS_EXPIRY_SOON 알림이 발송되고 notification_log에 기록된다.
 */
@UseCaseIT
class PassExpiryNotificationUseCaseIT {

    @Autowired PassExpiryBatchUseCase passExpiryBatchService;
    @Autowired NotificationOutboxDispatcher notificationOutboxDispatcher;
    @Autowired UserStorePort userStorePort;
    @Autowired PassPurchaseStorePort passPurchaseStorePort;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @MockitoSpyBean NotificationOutboxInsertPort notificationOutboxInsertPort;

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

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(0);
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }

    @DisplayName("서버가 예정 시각 뒤 재기동돼도 아직 만료 전인 8회권 알림을 보충한다")
    @Test
    void sendExpiryNotifications_afterScheduledTime_catchesUpBeforeExpiry() {
        User user = userStorePort.save(new User(
                "pass-expiry-catch-up@example.com", "hashed-password", "회원", "01056565656"));
        PassPurchase pass = passPurchaseStorePort.save(
                passPurchase(user.getId(), LocalDateTime.now(clock).plusDays(6), 0L));

        BatchResult result = passExpiryBatchService.sendExpiryNotifications();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(logs).singleElement().satisfies(log -> {
                softly.assertThat(log.getEventType()).isEqualTo(NotificationEventType.PASS_EXPIRY_SOON);
                softly.assertThat(log.getUserId()).isEqualTo(user.getId());
            });
            softly.assertThat(notificationOutboxRepository.findAll())
                    .singleElement()
                    .satisfies(outbox -> softly.assertThat(outbox.getAggregateId())
                            .isEqualTo(pass.getId()));
        });
    }

    @DisplayName("같은 회원의 8회권은 구매 건별로 만료 알림을 한 번씩 발송한다")
    @Test
    void sendExpiryNotifications_deduplicatesByPassId() {
        User user = userStorePort.save(new User("pass-expiry-dedupe@example.com", "hashed-password", "회원", "01077778888"));
        LocalDateTime target = LocalDateTime.now(clock).plusDays(7);
        PassPurchase firstPass = passPurchaseStorePort.save(passPurchase(user.getId(), target, 0L));

        BatchResult first = passExpiryBatchService.sendExpiryNotifications();
        awaitLogCount(notificationLogProbe, 1);
        PassPurchase secondPass = passPurchaseStorePort.save(passPurchase(user.getId(), target, 0L));
        BatchResult second = passExpiryBatchService.sendExpiryNotifications();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 2);
        BatchResult third = passExpiryBatchService.sendExpiryNotifications();
        awaitLogCount(notificationLogProbe, 2);

        assertSoftly(softly -> {
            softly.assertThat(first.successCount()).isEqualTo(1);
            softly.assertThat(first.failureCount()).isZero();
            softly.assertThat(second.successCount()).isEqualTo(1);
            softly.assertThat(second.failureCount()).isZero();
            softly.assertThat(third.successCount()).isZero();
            softly.assertThat(third.failureCount()).isZero();
            softly.assertThat(logs).extracting(NotificationLog::getUserId)
                    .containsOnly(user.getId());
            softly.assertThat(notificationOutboxRepository.findAll())
                    .extracting(outbox -> outbox.getAggregateId())
                    .containsExactlyInAnyOrder(firstPass.getId(), secondPass.getId());
        });
    }

    @DisplayName("한 8회권의 알림 저장이 실패해도 다른 8회권 알림은 커밋된다")
    @Test
    void sendExpiryNotifications_isolatesOutboxFailurePerPass() {
        User failingUser = userStorePort.save(new User(
                "pass-expiry-failure@example.com", "hashed-password", "회원", "01088889999"));
        User successfulUser = userStorePort.save(new User(
                "pass-expiry-success@example.com", "hashed-password", "회원", "01099990000"));
        LocalDateTime target = LocalDateTime.now(clock).plusDays(7);
        PassPurchase failingPass = passPurchaseStorePort.save(passPurchase(failingUser.getId(), target, 0L));
        PassPurchase successfulPass = passPurchaseStorePort.save(passPurchase(successfulUser.getId(), target, 0L));
        doThrow(new IllegalStateException("outbox 저장 실패"))
                .when(notificationOutboxInsertPort)
                .insertIfAbsent(argThat(outbox -> Objects.equals(outbox.getAggregateId(), failingPass.getId())));

        BatchResult result = passExpiryBatchService.sendExpiryNotifications();
        List<NotificationLog> logs = awaitLogCount(notificationLogProbe, 1);

        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(1);
            softly.assertThat(result.failureCount()).isEqualTo(1);
            softly.assertThat(logs).singleElement()
                    .satisfies(log -> softly.assertThat(log.getUserId()).isEqualTo(successfulUser.getId()));
            softly.assertThat(notificationOutboxRepository.findAll())
                    .singleElement()
                    .satisfies(outbox -> softly.assertThat(outbox.getAggregateId())
                            .isEqualTo(successfulPass.getId()));
        });
    }

    @DisplayName("발송 전에 잔여 횟수가 사라진 8회권 만료 알림은 외부 발송 없이 종결한다")
    @Test
    void dispatchExpiryReminder_withoutRemainingCredits_marksObsolete() {
        User user = userStorePort.save(new User(
                "pass-expiry-obsolete@example.com", "hashed-password", "회원", "01012121212"));
        LocalDateTime now = LocalDateTime.now(clock);
        PassPurchase pass = passPurchaseStorePort.save(
                passPurchase(user.getId(), now.plusDays(7), 0L));
        NotificationOutbox outbox = notificationOutboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        user.getId(),
                        NotificationEventType.PASS_EXPIRY_SOON,
                        "PASS_PURCHASE",
                        pass.getId()),
                now));
        pass.expire();
        passPurchaseStorePort.save(pass);

        BatchResult result = notificationOutboxDispatcher.dispatchPending();

        NotificationOutbox obsolete = notificationOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(obsolete.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE);
            softly.assertThat(obsolete.getLastError()).isEqualTo("REMINDER_NO_LONGER_ELIGIBLE");
            softly.assertThat(obsolete.getProcessedAt()).isEqualTo(now);
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }
}
