package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.port.in.NotificationFailureAdminUseCase;
import com.personal.happygallery.domain.notification.NotificationEventType;
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

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class NotificationFailureAdminUseCaseIT {

    @Autowired NotificationFailureAdminUseCase failureAdminUseCase;
    @Autowired NotificationOutboxRepository outboxRepository;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearUsers();
    }

    @DisplayName("최종 실패 알림은 관리자 목록에 나타나고 같은 outbox로 재처리된다")
    @Test
    void retryFailed_reopensSameOutbox() {
        User user = userStorePort.save(new User(
                "failed-notification@example.com", "hashed", "회원", "01012345678"));
        LocalDateTime now = LocalDateTime.now(clock);
        NotificationOutbox outbox = outboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        user.getId(), NotificationEventType.PASS_PURCHASED, "PASS_PURCHASE", 1L),
                now));
        for (int attempt = 0; attempt < 5; attempt++) {
            String processingToken = outbox.markProcessing(now);
            outbox.markDeliveryFailed(processingToken, "ALL_CHANNELS_FAILED", now, now, 5);
        }
        outboxRepository.saveAndFlush(outbox);

        var failed = failureAdminUseCase.listFailed();
        NotificationOutbox retried = failureAdminUseCase.retry(outbox.getId());

        assertSoftly(softly -> {
            softly.assertThat(failed).extracting(NotificationOutbox::getId).containsExactly(outbox.getId());
            softly.assertThat(retried.getId()).isEqualTo(outbox.getId());
            softly.assertThat(retried.getIdempotencyKey()).isEqualTo(outbox.getIdempotencyKey());
            softly.assertThat(retried.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
            softly.assertThat(retried.getAttemptCount()).isZero();
            softly.assertThat(retried.getLastError()).isNull();
        });
    }
}
