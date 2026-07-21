package com.personal.happygallery.application.notification;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationLogRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@UseCaseIT
class NotificationRetentionUseCaseIT {

    @Autowired NotificationRetentionService retentionService;
    @Autowired NotificationOutboxRepository outboxRepository;
    @Autowired NotificationLogRepository logRepository;
    @Autowired UserStorePort userStore;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearUsers();
    }

    @DisplayName("알림 보존 정리는 오래된 최종 outbox와 감사 로그만 삭제하고 재시도 상태는 보존한다")
    @Test
    void deleteExpiredNotifications_deletesOnlyOldTerminalData() {
        User user = userStore.save(new User(
                "notification-retention@example.com", "hash", "회원", "01044445555"));
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(180);

        NotificationOutbox expiredSent = outboxRepository.save(newOutbox(user.getId(), 1L, cutoff.minusDays(1)));
        String expiredToken = expiredSent.markProcessing(cutoff.minusDays(1));
        expiredSent.markSent(expiredToken, cutoff.minusSeconds(1));
        expiredSent = outboxRepository.save(expiredSent);

        NotificationOutbox retainedSent = outboxRepository.save(newOutbox(user.getId(), 2L, cutoff));
        String retainedToken = retainedSent.markProcessing(cutoff);
        retainedSent.markSent(retainedToken, cutoff.plusSeconds(1));
        retainedSent = outboxRepository.save(retainedSent);

        NotificationOutbox pending = outboxRepository.save(newOutbox(user.getId(), 3L, cutoff.minusDays(1)));
        NotificationOutbox processing = outboxRepository.save(newOutbox(user.getId(), 4L, cutoff.minusDays(1)));
        processing.markProcessing(cutoff.minusDays(1));
        processing = outboxRepository.save(processing);

        NotificationOutbox failed = outboxRepository.save(newOutbox(user.getId(), 5L, cutoff.minusDays(1)));
        String failedToken = failed.markProcessing(cutoff.minusDays(1));
        failed.markDeliveryFailed(
                failedToken, "FINAL", cutoff.minusDays(1), cutoff.minusSeconds(1), 1);
        failed = outboxRepository.save(failed);

        NotificationOutbox retainedFailed = outboxRepository.save(newOutbox(user.getId(), 6L, cutoff));
        String retainedFailedToken = retainedFailed.markProcessing(cutoff);
        retainedFailed.markDeliveryFailed(
                retainedFailedToken, "FINAL", cutoff, cutoff.plusSeconds(1), 1);
        retainedFailed = outboxRepository.save(retainedFailed);

        NotificationOutbox retryablePending = outboxRepository.save(
                newOutbox(user.getId(), 7L, cutoff.minusDays(1)));
        String retryableToken = retryablePending.markProcessing(cutoff.minusDays(1));
        retryablePending.markDeliveryFailed(
                retryableToken, "TEMPORARY", cutoff.minusDays(1), cutoff.minusSeconds(1), 2);
        retryablePending = outboxRepository.save(retryablePending);

        NotificationLog expiredLog = logRepository.save(NotificationLog.success(
                null, user.getId(), NotificationChannel.SMS,
                NotificationEventType.ORDER_PAID, cutoff.minusSeconds(1)));
        NotificationLog retainedLog = logRepository.save(NotificationLog.success(
                null, user.getId(), NotificationChannel.SMS,
                NotificationEventType.ORDER_PAID, cutoff.plusSeconds(1)));

        int deletedLogs = retentionService.deleteChannelLogsBefore(cutoff, 100);
        int deletedOutboxes = retentionService.deleteTerminalOutboxesBefore(cutoff, 100);

        Long expiredSentId = expiredSent.getId();
        Long retainedSentId = retainedSent.getId();
        Long processingId = processing.getId();
        Long failedId = failed.getId();
        Long retainedFailedId = retainedFailed.getId();
        Long retryablePendingId = retryablePending.getId();
        assertSoftly(softly -> {
            softly.assertThat(deletedLogs).isOne();
            softly.assertThat(deletedOutboxes).isEqualTo(2);
            softly.assertThat(logRepository.findById(expiredLog.getId())).isEmpty();
            softly.assertThat(logRepository.findById(retainedLog.getId())).isPresent();
            softly.assertThat(outboxRepository.findById(expiredSentId)).isEmpty();
            softly.assertThat(outboxRepository.findById(retainedSentId)).isPresent();
            softly.assertThat(outboxRepository.findById(pending.getId())).isPresent();
            softly.assertThat(outboxRepository.findById(processingId)).isPresent();
            softly.assertThat(outboxRepository.findById(failedId)).isEmpty();
            softly.assertThat(outboxRepository.findById(retainedFailedId)).isPresent();
            softly.assertThat(outboxRepository.findById(retryablePendingId))
                    .hasValueSatisfying(outbox -> softly.assertThat(outbox.getStatus())
                            .isEqualTo(NotificationOutboxStatus.PENDING));
        });
    }

    private static NotificationOutbox newOutbox(Long userId, Long aggregateId, LocalDateTime requestedAt) {
        return NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        userId, NotificationEventType.ORDER_PAID, "ORDER", aggregateId),
                requestedAt);
    }
}
