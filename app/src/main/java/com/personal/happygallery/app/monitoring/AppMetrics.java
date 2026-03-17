package com.personal.happygallery.app.monitoring;

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
 *   <li>라벨: {@code event_type} — 이벤트 유형 구분용 (고유값 금지)</li>
 * </ul>
 *
 * <h3>원칙</h3>
 * <ul>
 *   <li>{@code userId}, {@code orderId}, {@code phone} 같은 고유값은 label로 쓰지 않는다.</li>
 *   <li>시스템 메트릭(JVM, HTTP)은 Micrometer 자동 등록에 맡긴다.</li>
 *   <li>이 클래스는 비즈니스/전환 지표만 관리한다.</li>
 * </ul>
 */
@Component
public class AppMetrics {

    private final MeterRegistry registry;

    public AppMetrics(MeterRegistry registry) {
        this.registry = registry;
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
        Counter.builder("happygallery.funnel.guest_claim_completed")
                .description("비회원→회원 기록 인수 완료")
                .register(registry)
                .increment();
    }
}
