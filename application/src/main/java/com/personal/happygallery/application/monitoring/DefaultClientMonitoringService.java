package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultClientMonitoringService implements ClientMonitoringUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultClientMonitoringService.class);

    private final AppMetrics appMetrics;

    public DefaultClientMonitoringService(AppMetrics appMetrics) {
        this.appMetrics = appMetrics;
    }

    @Override
    public void captureFrontendEvent(ClientMonitoringEventType eventType,
                                     String path,
                                     String source,
                                     String target,
                                     Long userId) {
        // path/source/target은 클라이언트가 임의로 보낼 수 있어 로그에 남기지 않는다.
        log.info("[client-monitoring] event={} authenticated={} userId={}",
                eventType.logValue(),
                userId != null,
                userId);
        appMetrics.incrementClientEvent(eventType.logValue());
    }

    /** guest claim 완료는 내부 서비스가 호출하는 모니터링 헬퍼다. */
    @Override
    public void logGuestClaimCompleted(Long userId,
                                       Long guestId,
                                       int claimedOrderCount,
                                       int claimedBookingCount) {
        log.info("[client-monitoring] event={} path={} source={} target={} authenticated=true userId={} guestId={} orders={} bookings={}",
                ClientMonitoringEventType.GUEST_CLAIM_COMPLETED.logValue(),
                "/api/v1/me/guest-claims",
                "guest_claim_submit",
                "claim_completed",
                userId,
                guestId,
                claimedOrderCount,
                claimedBookingCount);
        appMetrics.incrementGuestClaimCompleted();
    }

}
