package com.personal.happygallery.application.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    private final MeterRegistry registry;
    private final Counter guestClaimCompleted;
    private final Counter paymentConfirmReconciliationRequired;
    private final Counter notificationOutboxFailed;
    private final Counter customerSessionRevocationFailed;

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
        this.customerSessionRevocationFailed = Counter.builder(
                        "happygallery.customer.session.revocation_failed")
                .description("회원 자격 증명 변경 후 이전 버전 Redis 세션 폐기 실패")
                .register(registry);
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

    /** 자격 증명 변경 이후 이전 버전 Redis 회원 세션 삭제가 실패한 건수를 기록한다. */
    public void incrementCustomerSessionRevocationFailure() {
        customerSessionRevocationFailed.increment();
    }
}
