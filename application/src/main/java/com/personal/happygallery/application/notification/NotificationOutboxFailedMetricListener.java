package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.monitoring.AppMetrics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class NotificationOutboxFailedMetricListener {

    private final AppMetrics appMetrics;

    NotificationOutboxFailedMetricListener(AppMetrics appMetrics) {
        this.appMetrics = appMetrics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recordFinalFailure(NotificationOutboxFailedEvent event) {
        appMetrics.incrementNotificationOutboxFailed();
    }
}
