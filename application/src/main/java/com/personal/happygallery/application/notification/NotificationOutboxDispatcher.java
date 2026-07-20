package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
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
    private static final int PROCESSING_TIMEOUT_MINUTES = 1;
    private static final String TRANSIENT_DELIVERY_FAILURE = "TRANSIENT_DELIVERY_FAILURE";
    private static final String PERMANENT_DELIVERY_FAILURE = "PERMANENT_DELIVERY_FAILURE";
    private static final String DELIVERY_FAILED = "DELIVERY_FAILED";
    private static final String AUDIT_LOG_PERSISTENCE_FAILED = "AUDIT_LOG_PERSISTENCE_FAILED";
    private static final String DISPATCH_EXCEPTION = "DISPATCH_EXCEPTION";

    private final NotificationOutboxTransactionService transactionService;
    private final NotificationService notificationService;

    public NotificationOutboxDispatcher(NotificationOutboxTransactionService transactionService,
                                        NotificationService notificationService) {
        this.transactionService = transactionService;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.NEVER)
    public BatchResult dispatchPending() {
        List<NotificationOutboxReservation> reservations = transactionService.reserveDispatchable(
                DISPATCH_LIMIT, PROCESSING_TIMEOUT_MINUTES);
        int successCount = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();

        for (NotificationOutboxReservation reservation : reservations) {
            try {
                switch (dispatchReserved(reservation)) {
                    case SENT -> successCount++;
                    case FAILED -> failureReasons.merge(DELIVERY_FAILED, 1, Integer::sum);
                    case STALE -> log.info("[알림 outbox] 오래된 실행 결과 무시 [outboxId={}]",
                            reservation.outboxId());
                }
            } catch (Exception e) {
                log.warn("[알림 outbox] dispatch 실패 [outboxId={} type={}]",
                        reservation.outboxId(), e.getClass().getSimpleName());
                if (recordDispatchException(reservation, e)) {
                    failureReasons.merge(e.getClass().getSimpleName(), 1, Integer::sum);
                }
            }
        }

        return BatchResult.of(successCount, failureReasons);
    }

    private DispatchOutcome dispatchReserved(NotificationOutboxReservation reservation) {
        var request = transactionService.loadRequest(
                reservation.outboxId(), reservation.processingToken());
        if (request.isEmpty()) {
            return DispatchOutcome.STALE;
        }
        NotificationOutboxDeliveryRequest delivery = request.get();
        NotificationSendResult result;
        try {
            result = switch (delivery.recipientType()) {
                case GUEST -> notificationService.sendByGuestId(
                        delivery.guestId(), delivery.eventType(), delivery.idempotencyKey());
                case USER -> notificationService.sendByUserId(
                        delivery.userId(), delivery.eventType(), delivery.idempotencyKey());
            };
        } catch (NotificationAuditPersistenceException exception) {
            if (exception.deliveryCompleted()) {
                return transactionService.markSentWithAuditFailure(
                        reservation.outboxId(), reservation.processingToken(), AUDIT_LOG_PERSISTENCE_FAILED)
                        ? DispatchOutcome.SENT
                        : DispatchOutcome.STALE;
            }
            return transactionService.markDeliveryFailed(
                    reservation.outboxId(),
                    reservation.processingToken(),
                    TRANSIENT_DELIVERY_FAILURE + ":" + AUDIT_LOG_PERSISTENCE_FAILED,
                    MAX_ATTEMPTS)
                    ? DispatchOutcome.FAILED
                    : DispatchOutcome.STALE;
        }

        return switch (result) {
            case SUCCESS -> transactionService.markSent(
                    reservation.outboxId(), reservation.processingToken())
                    ? DispatchOutcome.SENT
                    : DispatchOutcome.STALE;
            case TRANSIENT_FAILURE -> transactionService.markDeliveryFailed(
                    reservation.outboxId(),
                    reservation.processingToken(),
                    TRANSIENT_DELIVERY_FAILURE,
                    MAX_ATTEMPTS)
                    ? DispatchOutcome.FAILED
                    : DispatchOutcome.STALE;
            case PERMANENT_FAILURE -> transactionService.markPermanentFailure(
                    reservation.outboxId(),
                    reservation.processingToken(),
                    PERMANENT_DELIVERY_FAILURE)
                    ? DispatchOutcome.FAILED
                    : DispatchOutcome.STALE;
        };
    }

    private boolean recordDispatchException(NotificationOutboxReservation reservation,
                                            Exception dispatchFailure) {
        try {
            return transactionService.markDeliveryFailed(
                    reservation.outboxId(),
                    reservation.processingToken(),
                    DISPATCH_EXCEPTION + ":" + dispatchFailure.getClass().getSimpleName(),
                    MAX_ATTEMPTS);
        } catch (Exception recordingFailure) {
            dispatchFailure.addSuppressed(recordingFailure);
            log.error("[알림 outbox] dispatch 실패 기록 불가 [outboxId={} type={}]",
                    reservation.outboxId(), recordingFailure.getClass().getSimpleName(), recordingFailure);
            return false;
        }
    }

    private enum DispatchOutcome {
        SENT,
        FAILED,
        STALE
    }
}
