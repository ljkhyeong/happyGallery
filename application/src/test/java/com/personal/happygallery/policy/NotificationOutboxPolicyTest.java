package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class NotificationOutboxPolicyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 2, 10, 0);

    @DisplayName("대기 중이거나 처리 시간이 만료된 알림은 새 실행권으로 선점할 수 있다")
    @Test
    void markProcessing_allowsPendingAndProcessingOutbox() {
        NotificationOutbox outbox = newOutbox();

        String firstToken = outbox.markProcessing(NOW);
        String secondToken = outbox.reclaimProcessing(
                NOW.plusMinutes(2), NOW.plusMinutes(1));

        assertSoftly(softly -> {
            softly.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING);
            softly.assertThat(secondToken).isNotEqualTo(firstToken);
            softly.assertThat(outbox.getProcessingToken()).isEqualTo(secondToken);
            softly.assertThat(outbox.getLockedAt()).isEqualTo(NOW.plusMinutes(2));
        });
    }

    @DisplayName("처리 시간이 지나지 않은 알림은 활성 실행권을 유지한다")
    @Test
    void reclaimProcessing_rejectsActiveLease() {
        NotificationOutbox outbox = newOutbox();
        String token = outbox.markProcessing(NOW);

        assertConflict(() -> outbox.reclaimProcessing(
                NOW.plusSeconds(30), NOW.minusSeconds(30)));

        assertSoftly(softly -> {
            softly.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING);
            softly.assertThat(outbox.getProcessingToken()).isEqualTo(token);
            softly.assertThat(outbox.getLockedAt()).isEqualTo(NOW);
        });
    }

    @DisplayName("발송 완료 알림은 다시 처리 중으로 되돌릴 수 없다")
    @Test
    void markProcessing_rejectsSentOutbox() {
        NotificationOutbox outbox = newOutbox();
        String token = outbox.markProcessing(NOW);
        outbox.markSent(token, NOW.plusSeconds(1));

        assertConflict(() -> outbox.markProcessing(NOW.plusMinutes(2)));
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
    }

    @DisplayName("최종 실패 알림은 관리자 재처리 전에는 다시 선점할 수 없다")
    @Test
    void markProcessing_rejectsFailedOutbox() {
        NotificationOutbox outbox = newOutbox();
        String token = outbox.markProcessing(NOW);
        outbox.markDeliveryFailed(
                token, "모든 채널 실패", NOW.plusMinutes(1), NOW.plusSeconds(1), 1);

        assertConflict(() -> outbox.markProcessing(NOW.plusMinutes(2)));
        assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED);
    }

    @DisplayName("현재 처리 토큰으로만 알림을 OBSOLETE로 변경하고 직접 재선점을 거절한다")
    @Test
    void markObsolete_requiresCurrentProcessingTokenAndTerminatesOutbox() {
        NotificationOutbox outbox = newOutbox();
        String token = outbox.markProcessing(NOW);

        boolean staleAccepted = outbox.markObsolete(
                "stale-token", NOW.plusSeconds(1), "REMINDER_NO_LONGER_ELIGIBLE");
        boolean currentAccepted = outbox.markObsolete(
                token, NOW.plusSeconds(2), "REMINDER_NO_LONGER_ELIGIBLE");

        assertSoftly(softly -> {
            softly.assertThat(staleAccepted).isFalse();
            softly.assertThat(currentAccepted).isTrue();
            softly.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE);
            softly.assertThat(outbox.getProcessedAt()).isEqualTo(NOW.plusSeconds(2));
            softly.assertThat(outbox.getLastError()).isEqualTo("REMINDER_NO_LONGER_ELIGIBLE");
            softly.assertThat(outbox.getProcessingToken()).isNull();
            softly.assertThat(outbox.getLockedAt()).isNull();
        });
        assertConflict(() -> outbox.markProcessing(NOW.plusMinutes(2)));
    }

    @DisplayName("발송 준비는 현재 실행권만 게스트 수신자를 회원으로 바꾸고 lease를 연장한다")
    @Test
    void refreshRecipient_requiresCurrentTokenAndRenewsLease() {
        NotificationOutbox outbox = NotificationOutbox.from(
                NotificationRequestedEvent.forGuestOncePerAggregate(
                        10L, NotificationEventType.REMINDER_D1, "BOOKING", 20L),
                NOW.minusMinutes(1));
        String token = outbox.markProcessing(NOW);

        boolean staleAccepted = outbox.refreshRecipient(
                "stale-token", NotificationRecipientType.USER, null, 30L, NOW.plusSeconds(1));
        boolean currentAccepted = outbox.refreshRecipient(
                token, NotificationRecipientType.USER, null, 30L, NOW.plusSeconds(2));

        assertSoftly(softly -> {
            softly.assertThat(staleAccepted).isFalse();
            softly.assertThat(currentAccepted).isTrue();
            softly.assertThat(outbox.getRecipientType()).isEqualTo(NotificationRecipientType.USER);
            softly.assertThat(outbox.getGuestId()).isNull();
            softly.assertThat(outbox.getUserId()).isEqualTo(30L);
            softly.assertThat(outbox.getLockedAt()).isEqualTo(NOW.plusSeconds(2));
            softly.assertThat(outbox.getProcessingToken()).isEqualTo(token);
        });
    }

    @DisplayName("일정이 다시 유효해진 리마인드는 같은 멱등 행을 새 수신자로 다시 연다")
    @Test
    void reactivateObsolete_reusesAggregateIdempotencyAndRefreshesRecipient() {
        NotificationOutbox outbox = NotificationOutbox.from(
                NotificationRequestedEvent.forGuestOncePerAggregate(
                        10L, NotificationEventType.REMINDER_D1, "BOOKING", 20L),
                NOW);
        String token = outbox.markProcessing(NOW);
        outbox.markObsolete(token, NOW.plusSeconds(1), "REMINDER_NO_LONGER_ELIGIBLE");

        boolean reactivated = outbox.reactivateObsolete(
                NotificationRequestedEvent.forUserOncePerAggregate(
                        30L, NotificationEventType.REMINDER_D1, "BOOKING", 20L),
                NOW.plusDays(1));

        assertSoftly(softly -> {
            softly.assertThat(reactivated).isTrue();
            softly.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING);
            softly.assertThat(outbox.getRecipientType().name()).isEqualTo("USER");
            softly.assertThat(outbox.getGuestId()).isNull();
            softly.assertThat(outbox.getUserId()).isEqualTo(30L);
            softly.assertThat(outbox.getAttemptCount()).isZero();
            softly.assertThat(outbox.getNextAttemptAt()).isEqualTo(NOW.plusDays(1));
            softly.assertThat(outbox.getProcessedAt()).isNull();
            softly.assertThat(outbox.getLastError()).isNull();
        });
    }

    private NotificationOutbox newOutbox() {
        return NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        1L, NotificationEventType.PASS_PURCHASED, "PASS_PURCHASE", 1L),
                NOW);
    }

    private void assertConflict(Runnable command) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }
}
