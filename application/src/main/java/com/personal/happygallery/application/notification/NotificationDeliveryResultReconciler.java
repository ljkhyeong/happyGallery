package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResult;
import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResultProvider;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationDeliveryResultReconciler {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryResultReconciler.class);
    private static final int RECONCILE_LIMIT = 50;
    private static final int PROCESSING_TIMEOUT_MINUTES = 1;
    private static final String PROVIDER_RESULT_FAILED = "PROVIDER_RESULT_FAILED";

    private final NotificationOutboxTransactionService transactionService;
    private final NotificationService notificationService;
    private final Map<NotificationChannel, NotificationDeliveryResultProvider> providers;

    public NotificationDeliveryResultReconciler(
            NotificationOutboxTransactionService transactionService,
            NotificationService notificationService,
            List<NotificationDeliveryResultProvider> providers) {
        this.transactionService = transactionService;
        this.notificationService = notificationService;
        this.providers = new EnumMap<>(NotificationChannel.class);
        providers.forEach(provider -> this.providers.put(provider.channel(), provider));
    }

    @Transactional(propagation = Propagation.NEVER)
    public BatchResult reconcilePending() {
        int successCount = 0;
        Map<String, Integer> failureReasons = new LinkedHashMap<>();
        for (int count = 0; count < RECONCILE_LIMIT; count++) {
            var reservation = transactionService.reserveNextDeliveryResult(PROCESSING_TIMEOUT_MINUTES);
            if (reservation.isEmpty()) {
                break;
            }
            try {
                switch (reconcile(reservation.get())) {
                    case DELIVERED -> successCount++;
                    case FAILED -> failureReasons.merge(PROVIDER_RESULT_FAILED, 1, Integer::sum);
                    case PENDING, STALE -> { }
                }
            } catch (Exception exception) {
                log.warn("[알림 결과] 확인 예외 [outboxId={} type={}]",
                        reservation.get().outboxId(), exception.getClass().getSimpleName());
                transactionService.rescheduleDeliveryResult(
                        reservation.get().outboxId(),
                        reservation.get().processingToken(),
                        exception.getClass().getSimpleName());
                failureReasons.merge(exception.getClass().getSimpleName(), 1, Integer::sum);
            }
        }
        return BatchResult.of(successCount, failureReasons);
    }

    private ReconcileOutcome reconcile(NotificationDeliveryResultReservation reservation) {
        NotificationDeliveryResultProvider provider = providers.get(reservation.providerChannel());
        if (provider == null) {
            return transactionService.rescheduleDeliveryResult(
                    reservation.outboxId(), reservation.processingToken(), "PROVIDER_NOT_CONFIGURED")
                    ? ReconcileOutcome.PENDING
                    : ReconcileOutcome.STALE;
        }
        NotificationDeliveryResult result = provider.findResult(
                reservation.providerRequestId(), reservation.providerRecipientSeq());
        return switch (result.status()) {
            case DELIVERED -> transactionService.markProviderDelivered(reservation)
                    ? ReconcileOutcome.DELIVERED
                    : ReconcileOutcome.STALE;
            case PENDING, UNAVAILABLE -> transactionService.rescheduleDeliveryResult(
                    reservation.outboxId(), reservation.processingToken(), result.reason())
                    ? ReconcileOutcome.PENDING
                    : ReconcileOutcome.STALE;
            case FAILED -> fallbackOrFail(reservation, result.reason());
        };
    }

    private ReconcileOutcome fallbackOrFail(
            NotificationDeliveryResultReservation reservation, String reason) {
        if (!transactionService.markProviderFailedAudit(reservation, reason)) {
            return ReconcileOutcome.STALE;
        }
        if (reservation.providerChannel() != NotificationChannel.KAKAO) {
            return transactionService.markPermanentFailure(
                    reservation.outboxId(), reservation.processingToken(),
                    PROVIDER_RESULT_FAILED + ":" + reason)
                    ? ReconcileOutcome.FAILED
                    : ReconcileOutcome.STALE;
        }

        NotificationDeliveryAttempt fallback = switch (reservation.recipientType()) {
            case GUEST -> notificationService.sendByGuestIdWithOutcome(
                    reservation.guestId(),
                    reservation.eventType(),
                    reservation.idempotencyKey(),
                    NotificationChannel.KAKAO);
            case USER -> notificationService.sendByUserIdWithOutcome(
                    reservation.userId(),
                    reservation.eventType(),
                    reservation.idempotencyKey(),
                    NotificationChannel.KAKAO);
        };
        if (fallback.result() == NotificationSendResult.ACCEPTED) {
            return transactionService.markDeliveryPending(
                    reservation.outboxId(),
                    reservation.processingToken(),
                    fallback.channel(),
                    fallback.providerRequestId(),
                    fallback.providerRecipientSeq(),
                    null)
                    ? ReconcileOutcome.PENDING
                    : ReconcileOutcome.STALE;
        }
        if (fallback.result() == NotificationSendResult.SUCCESS) {
            return transactionService.markSent(
                    reservation.outboxId(), reservation.processingToken())
                    ? ReconcileOutcome.DELIVERED
                    : ReconcileOutcome.STALE;
        }
        return transactionService.markPermanentFailure(
                reservation.outboxId(),
                reservation.processingToken(),
                "FALLBACK_" + fallback.result())
                ? ReconcileOutcome.FAILED
                : ReconcileOutcome.STALE;
    }

    private enum ReconcileOutcome {
        DELIVERED,
        PENDING,
        FAILED,
        STALE
    }
}
