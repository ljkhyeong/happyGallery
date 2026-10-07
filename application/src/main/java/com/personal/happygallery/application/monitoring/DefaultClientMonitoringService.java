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
    public void captureFrontendEvent(ClientMonitoringEventType eventType, Long userId) {
        log.info("[client-monitoring] event={} authenticated={} userId={}",
                eventType.logValue(),
                userId != null,
                userId);
        appMetrics.incrementClientEvent(eventType.logValue());
    }
}
