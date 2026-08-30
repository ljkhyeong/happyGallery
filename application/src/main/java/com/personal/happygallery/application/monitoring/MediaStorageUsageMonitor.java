package com.personal.happygallery.application.monitoring;

import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MediaStorageUsageMonitor {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageUsageMonitor.class);

    private final ImageMediaStoragePort storagePort;
    private final AppMetrics metrics;

    public MediaStorageUsageMonitor(ImageMediaStoragePort storagePort, AppMetrics metrics) {
        this.storagePort = storagePort;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelayString = "${app.media.usage-refresh-ms:300000}")
    public void refresh() {
        try {
            metrics.recordMediaStorageUsage(storagePort.usedBytes());
        } catch (RuntimeException exception) {
            metrics.incrementMediaStorageRefreshFailure();
            log.warn("[미디어 저장소] 사용량 갱신 실패 [type={}]", exception.getClass().getSimpleName());
        }
    }
}
