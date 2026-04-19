package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultClientMonitoringService implements ClientMonitoringUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultClientMonitoringService.class);

    private final AppMetrics appMetrics;

    public DefaultClientMonitoringService(AppMetrics appMetrics) {
        this.appMetrics = appMetrics;
    }

    public void captureFrontendEvent(ClientMonitoringEventType eventType,
                                     String path,
                                     String source,
                                     String target,
                                     Long userId) {
        log.info("[client-monitoring] event={} path={} source={} target={} authenticated={} userId={}",
                eventType.logValue(),
                sanitize(path, 120),
                sanitizeOrDash(source, 80),
                sanitizeOrDash(target, 80),
                userId != null,
                userId);
        appMetrics.incrementClientEvent(eventType.logValue());
    }

    /** guest claim 완료는 내부 서비스가 호출하는 모니터링 헬퍼다. */
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

    private static String sanitizeOrDash(String value, int maxLength) {
        return StringUtils.hasText(value) ? sanitize(value, maxLength) : "-";
    }

    private static String sanitize(String value, int maxLength) {
        String normalized = value.replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
