package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxBacklogSummary;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptBacklogSummary;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundBacklogSummary;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.payment.RefundStatus;
import com.personal.happygallery.domain.time.Clocks;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalBacklogMetricsTest {

    @DisplayName("backlog 조회 실패는 기존 값을 0으로 덮지 않고 갱신 지연과 실패 횟수로 드러낸다")
    @Test
    void refresh_queryFails_preservesLastSnapshotAndExposesStaleness() {
        RefundPort refundPort = mock(RefundPort.class);
        NotificationOutboxPort outboxPort = mock(NotificationOutboxPort.class);
        PaymentAttemptReaderPort paymentAttemptReader = mock(PaymentAttemptReaderPort.class);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MutableClock clock = new MutableClock(Instant.parse("2026-07-20T00:00:00Z"));
        OperationalBacklogMetrics metrics = new OperationalBacklogMetrics(
                refundPort, outboxPort, paymentAttemptReader, registry, clock);
        when(paymentAttemptReader.summarizeReconciliationRequiredBacklog())
                .thenReturn(new PaymentAttemptBacklogSummary(
                        2, LocalDateTime.of(2026, 7, 20, 8, 58)));
        when(refundPort.summarizeUnresolvedBacklog()).thenReturn(List.of(
                new RefundBacklogSummary(
                        RefundStatus.RETRYABLE,
                        2,
                        LocalDateTime.of(2026, 7, 20, 9, 1)),
                new RefundBacklogSummary(
                        RefundStatus.REQUESTED,
                        1,
                        LocalDateTime.of(2026, 7, 19, 23, 50))));
        when(outboxPort.summarizeUnresolvedBacklog()).thenReturn(List.of(
                new NotificationOutboxBacklogSummary(
                        NotificationOutboxStatus.PENDING,
                        3,
                        LocalDateTime.of(2026, 7, 20, 9, 1)),
                new NotificationOutboxBacklogSummary(
                        NotificationOutboxStatus.PROCESSING,
                        1,
                        LocalDateTime.of(2026, 7, 20, 8, 59, 30))));

        assertThat(gauge(registry, "happygallery.refund.backlog.count", "status",
                "retryable")).isNaN();

        metrics.refresh();

        assertSoftly(softly -> {
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.count", "status",
                    "retryable")).isEqualTo(2);
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.oldest.age", "status",
                    "retryable")).isZero();
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.oldest.age", "status",
                    "requested")).isEqualTo(600);
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.count", "status",
                    "failed")).isZero();
            softly.assertThat(gauge(registry, "happygallery.notification.outbox.backlog.count", "status",
                    "pending")).isEqualTo(3);
            softly.assertThat(gauge(registry, "happygallery.notification.outbox.backlog.oldest.age", "status",
                    "pending")).isZero();
            softly.assertThat(gauge(registry, "happygallery.notification.outbox.backlog.oldest.age", "status",
                    "processing")).isEqualTo(30);
            softly.assertThat(gauge(
                    registry, "happygallery.payment.confirm.reconciliation.backlog.count"))
                    .isEqualTo(2);
            softly.assertThat(gauge(
                    registry, "happygallery.payment.confirm.reconciliation.backlog.oldest.age"))
                    .isEqualTo(120);
        });

        when(paymentAttemptReader.summarizeReconciliationRequiredBacklog())
                .thenThrow(new IllegalStateException("database unavailable"));
        when(refundPort.summarizeUnresolvedBacklog())
                .thenThrow(new IllegalStateException("database unavailable"));
        when(outboxPort.summarizeUnresolvedBacklog()).thenReturn(List.of());
        clock.advance(Duration.ofSeconds(90));

        metrics.refresh();

        assertSoftly(softly -> {
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.count", "status",
                    "retryable")).isEqualTo(2);
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.oldest.age", "status",
                    "retryable")).isEqualTo(30);
            softly.assertThat(gauge(registry, "happygallery.refund.backlog.oldest.age", "status",
                    "requested")).isEqualTo(690);
            softly.assertThat(gauge(registry, "happygallery.operational.backlog.refresh.age", "source",
                    "refund")).isEqualTo(90);
            softly.assertThat(registry.counter(
                    "happygallery.operational.backlog.refresh.failures", "source", "refund").count())
                    .isOne();
            softly.assertThat(gauge(registry, "happygallery.notification.outbox.backlog.count", "status",
                    "pending")).isZero();
            softly.assertThat(gauge(registry, "happygallery.operational.backlog.refresh.age", "source",
                    "notification")).isZero();
            softly.assertThat(gauge(
                    registry, "happygallery.payment.confirm.reconciliation.backlog.count"))
                    .isEqualTo(2);
            softly.assertThat(gauge(registry, "happygallery.operational.backlog.refresh.age", "source",
                    "payment")).isEqualTo(90);
            softly.assertThat(registry.counter(
                    "happygallery.operational.backlog.refresh.failures", "source", "payment").count())
                    .isOne();
        });
    }

    private static double gauge(SimpleMeterRegistry registry, String name) {
        return registry.get(name).gauge().value();
    }

    private static double gauge(SimpleMeterRegistry registry, String name, String tag, String value) {
        return registry.get(name).tag(tag, value).gauge().value();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return Clocks.SEOUL;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
