package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.batch.BatchResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDispatcher.class);
    private static final int DISPATCH_LIMIT = 50;
    private static final int MAX_ATTEMPTS = 5;
    private static final int PROCESSING_TIMEOUT_MINUTES = 10;
    private static final String ALL_CHANNELS_FAILED = "ALL_CHANNELS_FAILED";

    private final NotificationOutboxTransactionService transactionService;
    private final NotificationService notificationService;

    public NotificationOutboxDispatcher(NotificationOutboxTransactionService transactionService,
                                        NotificationService notificationService) {
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public BatchResult dispatchPending() {
        List<Long> outboxIds = transactionService.reserveDispatchableIds(
                DISPATCH_LIMIT, PROCESSING_TIMEOUT_MINUTES);
        int successCount = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();

        for (Long outboxId : outboxIds) {
            try {
                if (dispatchReserved(outboxId)) {
                    successCount++;
                } else {
                    failureReasons.merge(ALL_CHANNELS_FAILED, 1, Integer::sum);
                }
            } catch (Exception e) {
                log.warn("[알림 outbox] dispatch 실패 [outboxId={} type={}]",
                        outboxId, e.getClass().getSimpleName());
                failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
            }
        }

        return BatchResult.of(successCount, failureReasons);
    }

    private boolean dispatchReserved(Long outboxId) {
        NotificationOutboxDeliveryRequest request = transactionService.loadRequest(outboxId);
        boolean sent = switch (request.recipientType()) {
            case GUEST -> notificationService.sendByGuestId(request.guestId(), request.eventType());
            case USER -> notificationService.sendByUserId(request.userId(), request.eventType());
        };

        if (sent) {
            transactionService.markSent(outboxId);
            return true;
        }
        transactionService.markDeliveryFailed(outboxId, ALL_CHANNELS_FAILED, MAX_ATTEMPTS);
        return false;
    }
}
