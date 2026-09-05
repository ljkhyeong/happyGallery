package com.personal.happygallery.application.product;

import com.personal.happygallery.application.notification.NotificationOutboxService;
import com.personal.happygallery.application.product.port.out.RestockAlertDeliveryPort;
import com.personal.happygallery.application.product.port.out.RestockAlertPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestockAlertTransactionService {
    private final RestockAlertPort alerts;
    private final RestockAlertDeliveryPort delivery;
    private final NotificationOutboxService outbox;

    public RestockAlertTransactionService(RestockAlertPort alerts, RestockAlertDeliveryPort delivery,
                                         NotificationOutboxService outbox) {
        this.alerts = alerts;
        this.delivery = delivery;
        this.outbox = outbox;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(Long id) {
        var found = alerts.findByIdForUpdate(id);
        if (found.isEmpty() || !found.get().isActive()) return false;
        var alert = found.get();
        var sentAt = delivery.findSentAt(id);
        if (sentAt.isPresent()) {
            alert.markNotified(sentAt.get());
            alerts.saveAndFlush(alert);
            return true;
        }
        if (delivery.findEligibleUserId(id).isEmpty()) return false;
        boolean enqueued = outbox.enqueue(NotificationRequestedEvent.forUser(alert.getUserId(),
                NotificationEventType.PRODUCT_RESTOCK_AVAILABLE, "RESTOCK_ALERT", id));
        if (enqueued) {
            alert.markQueued();
            alerts.saveAndFlush(alert);
        }
        return enqueued;
    }
}
