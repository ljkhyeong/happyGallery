package com.personal.happygallery.application.monitoring.port.in;

import com.personal.happygallery.application.monitoring.ClientMonitoringEventType;

public interface ClientMonitoringUseCase {

    void captureFrontendEvent(ClientMonitoringEventType eventType,
                              String path,
                              String source,
                              String target,
                              Long userId);

    void logGuestClaimCompleted(Long userId,
                                Long guestId,
                                int claimedOrderCount,
                                int claimedBookingCount);
}
