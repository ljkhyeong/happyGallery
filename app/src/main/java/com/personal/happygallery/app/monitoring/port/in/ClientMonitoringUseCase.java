package com.personal.happygallery.app.monitoring.port.in;

import com.personal.happygallery.app.monitoring.ClientMonitoringEventType;

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
