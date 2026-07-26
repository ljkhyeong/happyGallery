package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxBacklogSummary;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptBacklogSummary;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.RefundBacklogSummary;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OperationalBacklogMetrics {

    private static final Logger log = LoggerFactory.getLogger(OperationalBacklogMetrics.class);
    private static final List<RefundStatus> REFUND_BACKLOG_STATUSES = List.of(
            RefundStatus.REQUESTED,
            RefundStatus.PROCESSING,
            RefundStatus.RETRYABLE,
            RefundStatus.RECONCILIATION_REQUIRED,
            RefundStatus.FAILED);
    private static final List<NotificationOutboxStatus> OUTBOX_BACKLOG_STATUSES = List.of(
            NotificationOutboxStatus.PENDING,
            NotificationOutboxStatus.PROCESSING,
            NotificationOutboxStatus.FAILED);

    private final RefundPort refundPort;
    private final NotificationOutboxPort outboxPort;
    private final PaymentAttemptReaderPort paymentAttemptReader;
    private final Clock clock;
    private final BacklogState paymentReconciliationState = new BacklogState();
    private final EnumMap<RefundStatus, BacklogState> refundStates = new EnumMap<>(RefundStatus.class);
    private final EnumMap<NotificationOutboxStatus, BacklogState> outboxStates =
            new EnumMap<>(NotificationOutboxStatus.class);
    private final RefreshState refundRefresh;
    private final RefreshState outboxRefresh;
    private final RefreshState paymentRefresh;
    private final Counter refundRefreshFailures;
    private final Counter outboxRefreshFailures;
    private final Counter paymentRefreshFailures;

    public OperationalBacklogMetrics(RefundPort refundPort,
                                     NotificationOutboxPort outboxPort,
                                     PaymentAttemptReaderPort paymentAttemptReader,
                                     MeterRegistry registry,
                                     Clock clock) {
        this.refundPort = refundPort;
        this.outboxPort = outboxPort;
        this.paymentAttemptReader = paymentAttemptReader;
        this.clock = clock;
        this.refundRefresh = new RefreshState(clock);
        this.outboxRefresh = new RefreshState(clock);
        this.paymentRefresh = new RefreshState(clock);

        registerBacklogGauges(
                registry,
                "happygallery.payment.confirm.reconciliation.backlog",
                null,
                paymentReconciliationState,
                "수동 대사가 필요한 결제");

        REFUND_BACKLOG_STATUSES.forEach(status -> {
            BacklogState state = new BacklogState();
            refundStates.put(status, state);
            registerBacklogGauges(
                    registry,
                    "happygallery.refund.backlog",
                    status.name().toLowerCase(Locale.ROOT),
                    state,
                    "미완료 환불");
        });
        OUTBOX_BACKLOG_STATUSES.forEach(status -> {
            BacklogState state = new BacklogState();
            outboxStates.put(status, state);
            registerBacklogGauges(
                    registry,
                    "happygallery.notification.outbox.backlog",
                    status.name().toLowerCase(Locale.ROOT),
                    state,
                    "미완료 알림 outbox");
        });

        registerRefreshGauge(registry, "refund", refundRefresh);
        registerRefreshGauge(registry, "notification", outboxRefresh);
        registerRefreshGauge(registry, "payment", paymentRefresh);
        this.refundRefreshFailures = registerRefreshFailureCounter(registry, "refund");
        this.outboxRefreshFailures = registerRefreshFailureCounter(registry, "notification");
        this.paymentRefreshFailures = registerRefreshFailureCounter(registry, "payment");
    }

    @Scheduled(
            fixedDelayString = "${app.monitoring.backlog.refresh-delay-ms:15000}",
            initialDelayString = "${app.monitoring.backlog.initial-delay-ms:0}")
    public void refresh() {
        refreshPaymentReconciliationBacklog();
        refreshRefundBacklog();
        refreshOutboxBacklog();
    }

    private void refreshPaymentReconciliationBacklog() {
        try {
            PaymentAttemptBacklogSummary summary =
                    paymentAttemptReader.summarizeReconciliationRequiredBacklog();
            paymentReconciliationState.update(
                    summary.count(), summary.oldestActionAt(), clock.getZone());
            paymentRefresh.markSucceeded(clock);
        } catch (Exception e) {
            paymentRefreshFailures.increment();
            log.warn("운영 backlog 메트릭 갱신 실패 [source=payment type={}]",
                    e.getClass().getSimpleName());
        }
    }

    private void refreshRefundBacklog() {
        try {
            List<RefundBacklogSummary> summaries = refundPort.summarizeUnresolvedBacklog();
            EnumMap<RefundStatus, RefundBacklogSummary> snapshot = new EnumMap<>(RefundStatus.class);
            REFUND_BACKLOG_STATUSES.forEach(status -> snapshot.put(
                    status, new RefundBacklogSummary(status, 0, null)));
            summaries.forEach(summary -> snapshot.put(summary.status(), summary));
            REFUND_BACKLOG_STATUSES.forEach(status -> {
                RefundBacklogSummary summary = snapshot.get(status);
                ZoneId actionTimeZone = status == RefundStatus.REQUESTED || status == RefundStatus.FAILED
                        ? ZoneOffset.UTC
                        : clock.getZone();
                refundStates.get(status).update(
                        summary.count(), summary.oldestActionAt(), actionTimeZone);
            });
            refundRefresh.markSucceeded(clock);
        } catch (Exception e) {
            refundRefreshFailures.increment();
            log.warn("운영 backlog 메트릭 갱신 실패 [source=refund type={}]",
                    e.getClass().getSimpleName());
        }
    }

    private void refreshOutboxBacklog() {
        try {
            List<NotificationOutboxBacklogSummary> summaries = outboxPort.summarizeUnresolvedBacklog();
            EnumMap<NotificationOutboxStatus, NotificationOutboxBacklogSummary> snapshot =
                    new EnumMap<>(NotificationOutboxStatus.class);
            OUTBOX_BACKLOG_STATUSES.forEach(status -> snapshot.put(
                    status, new NotificationOutboxBacklogSummary(status, 0, null)));
            summaries.forEach(summary -> snapshot.put(summary.status(), summary));
            OUTBOX_BACKLOG_STATUSES.forEach(status -> {
                NotificationOutboxBacklogSummary summary = snapshot.get(status);
                outboxStates.get(status).update(
                        summary.count(), summary.oldestActionAt(), clock.getZone());
            });
            outboxRefresh.markSucceeded(clock);
        } catch (Exception e) {
            outboxRefreshFailures.increment();
            log.warn("운영 backlog 메트릭 갱신 실패 [source=notification type={}]",
                    e.getClass().getSimpleName());
        }
    }

    private void registerBacklogGauges(MeterRegistry registry,
                                       String metricPrefix,
                                       String status,
                                       BacklogState state,
                                       String descriptionPrefix) {
        Gauge.Builder<BacklogState> countGauge = Gauge.builder(
                        metricPrefix + ".count", state, BacklogState::count)
                .description(descriptionPrefix + " 건수");
        Gauge.Builder<BacklogState> oldestAgeGauge = Gauge.builder(
                        metricPrefix + ".oldest.age", state,
                        value -> value.oldestAgeSeconds(clock))
                .description(descriptionPrefix + " 처리 기준 시각의 최장 경과 시간")
                .baseUnit("seconds");
        if (status != null) {
            countGauge.tag("status", status);
            oldestAgeGauge.tag("status", status);
        }
        countGauge.register(registry);
        oldestAgeGauge.register(registry);
    }

    private void registerRefreshGauge(MeterRegistry registry, String source, RefreshState state) {
        Gauge.builder("happygallery.operational.backlog.refresh.age", state,
                        value -> value.ageSeconds(clock))
                .description("DB backlog 스냅샷 마지막 정상 갱신 후 경과 시간")
                .baseUnit("seconds")
                .tag("source", source)
                .register(registry);
    }

    private Counter registerRefreshFailureCounter(MeterRegistry registry, String source) {
        return Counter.builder("happygallery.operational.backlog.refresh.failures")
                .description("DB backlog 스냅샷 갱신 실패")
                .tag("source", source)
                .register(registry);
    }

    private static final class BacklogState {

        private final AtomicReference<BacklogSnapshot> snapshot = new AtomicReference<>();

        void update(long newCount, LocalDateTime oldestActionAt, ZoneId actionTimeZone) {
            long oldestActionAtEpochSecond = oldestActionAt == null
                    ? 0
                    : oldestActionAt.atZone(actionTimeZone).toEpochSecond();
            snapshot.set(new BacklogSnapshot(newCount, oldestActionAtEpochSecond));
        }

        double count() {
            BacklogSnapshot current = snapshot.get();
            return current == null ? Double.NaN : current.count();
        }

        double oldestAgeSeconds(Clock clock) {
            BacklogSnapshot current = snapshot.get();
            if (current == null) {
                return Double.NaN;
            }
            long oldestEpochSecond = current.oldestActionAtEpochSecond();
            return oldestEpochSecond == 0
                    ? 0
                    : Math.max(0, clock.instant().getEpochSecond() - oldestEpochSecond);
        }
    }

    private record BacklogSnapshot(long count, long oldestActionAtEpochSecond) {}

    private static final class RefreshState {

        private final AtomicLong lastSucceededAtEpochSecond;

        private RefreshState(Clock clock) {
            this.lastSucceededAtEpochSecond = new AtomicLong(clock.instant().getEpochSecond());
        }

        void markSucceeded(Clock clock) {
            lastSucceededAtEpochSecond.set(clock.instant().getEpochSecond());
        }

        double ageSeconds(Clock clock) {
            return Math.max(
                    0,
                    clock.instant().getEpochSecond() - lastSucceededAtEpochSecond.get());
        }
    }
}
