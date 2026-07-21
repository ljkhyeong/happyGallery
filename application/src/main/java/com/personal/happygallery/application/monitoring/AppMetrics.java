package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.batch.BatchResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 커스텀 메트릭 레지스트리.
 *
 * <h3>네이밍 규격</h3>
 * <ul>
 *   <li>접두사: {@code happygallery.}</li>
 *   <li>제품 전환 지표: {@code happygallery.funnel.*}</li>
 *   <li>결제 운영 지표: {@code happygallery.payment.*}</li>
 *   <li>라벨: {@code event_type} — 이벤트 유형 구분용 (고유값 금지)</li>
 * </ul>
 *
 * <h3>원칙</h3>
 * <ul>
 *   <li>{@code userId}, {@code orderId}, {@code phone} 같은 고유값은 label로 쓰지 않는다.</li>
 *   <li>시스템 메트릭(JVM, HTTP)은 Micrometer 자동 등록에 맡긴다.</li>
 *   <li>이 클래스는 비즈니스/전환 및 운영 조치가 필요한 애플리케이션 지표만 관리한다.</li>
 * </ul>
 */
@Component
public class AppMetrics {

    private static final List<String> MONITORED_BATCH_JOBS = List.of(
            "order_auto_refund",
            "pickup_expire",
            "pass_expiry",
            "pass_expiry_notification",
            "pickup_deadline_reminder",
            "booking_d1_reminder",
            "booking_same_day_reminder",
            "refund_recovery",
            "payment_confirm_recovery",
            "payment_attempt_expiry",
            "personal_data_retention");

    private final MeterRegistry registry;
    private final Counter guestClaimCompleted;
    private final Counter paymentConfirmReconciliationRequired;
    private final Counter notificationOutboxFailed;
    private final Counter notificationLogPersistenceFailed;
    private final Counter customerSessionRevocationFailed;
    private final Counter mediaStorageRefreshFailed;
    private final AtomicLong mediaStorageBytes = new AtomicLong();
    private final AtomicLong mediaStorageLastSuccessSeconds = new AtomicLong();
    private final ConcurrentMap<String, AtomicLong> batchLastSuccessSeconds = new ConcurrentHashMap<>();

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.guestClaimCompleted = Counter.builder("happygallery.funnel.guest_claim_completed")
                .description("비회원→회원 기록 인수 완료")
                .register(registry);
        this.paymentConfirmReconciliationRequired = Counter.builder(
                        "happygallery.payment.confirm.reconciliation_required")
                .description("수동 대사가 필요한 결제 confirm 시도")
                .register(registry);
        this.notificationOutboxFailed = Counter.builder("happygallery.notification.outbox.failed")
                .description("자동 재시도를 모두 소진한 알림 outbox")
                .register(registry);
        this.notificationLogPersistenceFailed = Counter.builder(
                        "happygallery.notification.log.persistence_failed")
                .description("외부 알림 결과의 notification_log 저장 실패")
                .register(registry);
        this.customerSessionRevocationFailed = Counter.builder(
                        "happygallery.customer.session.revocation_failed")
                .description("회원 자격 증명 변경 후 이전 버전 Redis 세션 폐기 실패")
                .register(registry);
        this.mediaStorageRefreshFailed = Counter.builder("happygallery.media.storage.refresh_failed")
                .description("이미지 저장소 사용량 갱신 실패")
                .register(registry);
        Gauge.builder("happygallery.media.storage", mediaStorageBytes, AtomicLong::get)
                .description("자가 호스팅 이미지 저장소 사용량")
                .baseUnit("bytes")
                .register(registry);
        Gauge.builder("happygallery.media.storage.last_success", mediaStorageLastSuccessSeconds, AtomicLong::get)
                .description("이미지 저장소 사용량을 마지막으로 정상 갱신한 Unix 시각")
                .baseUnit("seconds")
                .register(registry);
        MONITORED_BATCH_JOBS.forEach(this::batchLastSuccess);
    }

    /**
     * 클라이언트 모니터링 이벤트 카운터를 증가시킨다.
     *
     * @param eventType 이벤트 유형 (예: guest_lookup_hub_viewed)
     */
    public void incrementClientEvent(String eventType) {
        Counter.builder("happygallery.funnel.client_event")
                .description("프론트엔드 전환 퍼널 이벤트")
                .tag("event_type", eventType)
                .register(registry)
                .increment();
    }

    /**
     * guest claim 완료 카운터를 증가시킨다.
     */
    public void incrementGuestClaimCompleted() {
        guestClaimCompleted.increment();
    }

    /** PG 멱등 안전 기간을 지나 수동 결제 대사가 필요해진 건수를 기록한다. */
    public void incrementPaymentConfirmReconciliationRequired() {
        paymentConfirmReconciliationRequired.increment();
    }

    /** 자동 재시도를 모두 소진한 알림 outbox 건수를 기록한다. */
    public void incrementNotificationOutboxFailed() {
        notificationOutboxFailed.increment();
    }

    /** 외부 알림 결과의 notification_log 저장 실패 건수를 기록한다. */
    public void incrementNotificationLogPersistenceFailure() {
        notificationLogPersistenceFailed.increment();
    }

    /** 자격 증명 변경 이후 이전 버전 Redis 회원 세션 삭제가 실패한 건수를 기록한다. */
    public void incrementCustomerSessionRevocationFailure() {
        customerSessionRevocationFailed.increment();
    }

    public void recordMediaStorageUsage(long bytes) {
        mediaStorageBytes.set(bytes);
        mediaStorageLastSuccessSeconds.set(Instant.now().getEpochSecond());
    }

    public void incrementMediaStorageRefreshFailure() {
        mediaStorageRefreshFailed.increment();
    }

    /** 배치의 정상 또는 부분 실패 결과와 처리 건수를 기록한다. */
    public void recordBatchResult(String job, BatchResult result, long durationNanos) {
        String status = result.failureCount() == 0 ? "succeeded" : "partial";
        recordBatchRun(job, status, durationNanos);
        recordBatchItems(job, "succeeded", result.successCount());
        recordBatchItems(job, "failed", result.failureCount());
        if (result.failureCount() == 0) {
            recordBatchLastSuccess(job);
        }
    }

    /** BatchResult를 반환하지 않는 배치의 정상 완료를 기록한다. */
    public void recordBatchSuccess(String job, long durationNanos) {
        recordBatchRun(job, "succeeded", durationNanos);
        recordBatchLastSuccess(job);
    }

    /** 예외로 중단된 배치를 기록한다. */
    public void recordBatchFailure(String job, long durationNanos) {
        recordBatchRun(job, "failed", durationNanos);
    }

    private void recordBatchRun(String job, String status, long durationNanos) {
        Counter.builder("happygallery.batch.runs")
                .description("배치 실행 결과")
                .tag("job", job)
                .tag("status", status)
                .register(registry)
                .increment();
        Timer.builder("happygallery.batch.duration")
                .description("배치 실행 시간")
                .tag("job", job)
                .tag("status", status)
                .register(registry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private void recordBatchItems(String job, String outcome, int count) {
        if (count == 0) {
            return;
        }
        Counter.builder("happygallery.batch.items")
                .description("배치 항목 처리 결과")
                .tag("job", job)
                .tag("outcome", outcome)
                .register(registry)
                .increment(count);
    }

    private void recordBatchLastSuccess(String job) {
        batchLastSuccess(job).set(Instant.now().getEpochSecond());
    }

    private AtomicLong batchLastSuccess(String job) {
        return batchLastSuccessSeconds.computeIfAbsent(job, key -> {
            AtomicLong lastSuccess = new AtomicLong();
            Gauge.builder("happygallery.batch.last_success", lastSuccess, AtomicLong::get)
                    .description("배치의 마지막 정상 완료 Unix 시각")
                    .tag("job", key)
                    .baseUnit("seconds")
                    .register(registry);
            return lastSuccess;
        });
    }
}
