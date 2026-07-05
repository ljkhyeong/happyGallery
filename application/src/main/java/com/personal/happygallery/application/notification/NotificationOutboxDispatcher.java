package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.batch.BatchResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class NotificationOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxDispatcher.class);
    private static final int DISPATCH_LIMIT = 50;
    private static final int MAX_ATTEMPTS = 5;
    private static final int PROCESSING_TIMEOUT_MINUTES = 10;
    private static final String ALL_CHANNELS_FAILED = "ALL_CHANNELS_FAILED";

    private final NotificationOutboxTransactionService transactionService;
    private final NotificationService notificationService;
    private final Executor notificationExecutor;

    public NotificationOutboxDispatcher(NotificationOutboxTransactionService transactionService,
                                        NotificationService notificationService,
                                        @Qualifier("notificationExecutor") Executor notificationExecutor) {
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.notificationExecutor = notificationExecutor;
    }

    public void dispatchAsync() {
        notificationExecutor.execute(() -> {
            try {
                dispatchPending();
            } catch (Exception e) {
                log.warn("[알림 outbox] 비동기 dispatch 실패", e);
            }
        });
    }

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
                log.warn("[알림 outbox] dispatch 실패 [outboxId={}]", outboxId, e);
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
