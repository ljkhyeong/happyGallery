package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
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
