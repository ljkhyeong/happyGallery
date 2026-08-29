package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.batch.BatchResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxScheduler {

    private final NotificationOutboxDispatcher dispatcher;
    private final NotificationDeliveryResultReconciler deliveryResultReconciler;

    NotificationOutboxScheduler(
            NotificationOutboxDispatcher dispatcher,
            NotificationDeliveryResultReconciler deliveryResultReconciler) {
        this.dispatcher = dispatcher;
        this.deliveryResultReconciler = deliveryResultReconciler;
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.poll-delay-ms:5000}")
    public BatchResult dispatchPending() {
        return dispatcher.dispatchPending();
    }

    @Scheduled(fixedDelayString = "${app.notification.delivery-result.poll-delay-ms:10000}")
    public BatchResult reconcileDeliveryResults() {
        return deliveryResultReconciler.reconcilePending();
    }
}
