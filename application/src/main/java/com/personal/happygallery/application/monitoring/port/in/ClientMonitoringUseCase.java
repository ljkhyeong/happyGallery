package com.personal.happygallery.application.monitoring.port.in;

import com.personal.happygallery.application.monitoring.ClientMonitoringEventType;

public interface ClientMonitoringUseCase {

    void captureFrontendEvent(ClientMonitoringEventType eventType, Long userId);
}
