package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class NotificationOutboxTransactionService {

    private static final int MAX_BACKOFF_EXPONENT = 5;
    private static final String REMINDER_NO_LONGER_ELIGIBLE = "REMINDER_NO_LONGER_ELIGIBLE";

    private final NotificationOutboxPort outboxPort;
    private final NotificationReminderEligibility reminderEligibility;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    NotificationOutboxTransactionService(NotificationOutboxPort outboxPort,
                                         NotificationReminderEligibility reminderEligibility,
                                         ApplicationEventPublisher eventPublisher,
                                         Clock clock) {
        this.outboxPort = outboxPort;
        this.reminderEligibility = reminderEligibility;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationOutboxReservation> reserveNextDispatchable(int processingTimeoutMinutes) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime staleBefore = now.minusMinutes(processingTimeoutMinutes);
        return outboxPort.findDispatchable(now, staleBefore, 1).stream()
                .findFirst()
                .map(outbox -> new NotificationOutboxReservation(
                        outbox.getId(), reserve(outbox, now, staleBefore)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationOutboxDeliveryPreparation prepareDelivery(
            Long outboxId, String processingToken) {
        NotificationOutbox outbox = findOutboxForUpdate(outboxId);
        if (!outbox.isProcessingOwnedBy(processingToken)) {
            return NotificationOutboxDeliveryPreparation.stale();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        NotificationReminderRecipient recipient;
        if (outbox.getEventType().isTimeSensitiveReminder()) {
            var eligibleRecipient = reminderEligibility.findEligibleRecipient(outbox, now);
            if (eligibleRecipient.isEmpty()) {
                return outbox.markObsolete(processingToken, now, REMINDER_NO_LONGER_ELIGIBLE)
                        ? NotificationOutboxDeliveryPreparation.obsolete()
                        : NotificationOutboxDeliveryPreparation.stale();
            }
            recipient = eligibleRecipient.get();
        } else {
            recipient = new NotificationReminderRecipient(outbox.getGuestId(), outbox.getUserId());
        }

        boolean prepared = outbox.refreshRecipient(
                processingToken,
                recipient.recipientType(),
                recipient.guestId(),
                recipient.userId(),
                now);
        if (!prepared) {
            return NotificationOutboxDeliveryPreparation.stale();
        }
        return NotificationOutboxDeliveryPreparation.ready(new NotificationOutboxDeliveryRequest(
                outbox.getId(),
                outbox.getRecipientType(),
                outbox.getGuestId(),
                outbox.getUserId(),
                outbox.getEventType(),
                outbox.getAggregateType(),
                outbox.getAggregateId(),
                outbox.getIdempotencyKey()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSent(Long outboxId, String processingToken) {
        NotificationOutbox outbox = findOutboxForUpdate(outboxId);
        return outbox.markSent(processingToken, LocalDateTime.now(clock));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSentWithAuditFailure(Long outboxId, String processingToken, String reason) {
        NotificationOutbox outbox = findOutboxForUpdate(outboxId);
        return outbox.markSentWithAuditFailure(processingToken, LocalDateTime.now(clock), reason);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markDeliveryFailed(Long outboxId, String processingToken, String reason, int maxAttempts) {
        NotificationOutbox outbox = findOutboxForUpdate(outboxId);
        LocalDateTime now = LocalDateTime.now(clock);
        boolean accepted = outbox.markDeliveryFailed(
                processingToken, reason, nextAttemptAt(outbox, now), now, maxAttempts);
        if (accepted && outbox.getStatus() == NotificationOutboxStatus.FAILED) {
            eventPublisher.publishEvent(new NotificationOutboxFailedEvent(outbox.getId()));
        }
        return accepted;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markPermanentFailure(Long outboxId, String processingToken, String reason) {
        return markDeliveryFailed(outboxId, processingToken, reason, 1);
    }

    private String reserve(NotificationOutbox outbox,
                           LocalDateTime now,
                           LocalDateTime staleBefore) {
        if (outbox.getStatus() == NotificationOutboxStatus.PROCESSING) {
            return outbox.reclaimProcessing(now, staleBefore);
        }
        return outbox.markProcessing(now);
    }

    private NotificationOutbox findOutboxForUpdate(Long outboxId) {
        return outboxPort.findByIdForUpdate(outboxId)
                .orElseThrow(() -> new IllegalStateException("알림 outbox 미존재: " + outboxId));
    }

    private LocalDateTime nextAttemptAt(NotificationOutbox outbox, LocalDateTime now) {
        int backoffExponent = Math.clamp((long) outbox.getAttemptCount(), 0, MAX_BACKOFF_EXPONENT);
        long delayMinutes = 1L << backoffExponent;
        return now.plusMinutes(delayMinutes);
    }
}

record NotificationOutboxReservation(Long outboxId, String processingToken) {}

record NotificationOutboxDeliveryRequest(Long outboxId,
                                         NotificationRecipientType recipientType,
                                         Long guestId,
                                         Long userId,
                                         NotificationEventType eventType,
                                         String aggregateType,
                                         Long aggregateId,
                                         String idempotencyKey) {}

record NotificationOutboxDeliveryPreparation(NotificationOutboxPreparationStatus status,
                                             NotificationOutboxDeliveryRequest delivery) {

    NotificationOutboxDeliveryPreparation {
        if ((status == NotificationOutboxPreparationStatus.READY) != (delivery != null)) {
            throw new IllegalArgumentException("발송 준비 완료 상태에만 delivery가 있어야 합니다.");
        }
    }

    static NotificationOutboxDeliveryPreparation ready(NotificationOutboxDeliveryRequest delivery) {
        return new NotificationOutboxDeliveryPreparation(
                NotificationOutboxPreparationStatus.READY, delivery);
    }

    static NotificationOutboxDeliveryPreparation obsolete() {
        return new NotificationOutboxDeliveryPreparation(
                NotificationOutboxPreparationStatus.OBSOLETE, null);
    }

    static NotificationOutboxDeliveryPreparation stale() {
        return new NotificationOutboxDeliveryPreparation(
                NotificationOutboxPreparationStatus.STALE, null);
    }
}

enum NotificationOutboxPreparationStatus {
    READY,
    OBSOLETE,
    STALE
}

record NotificationOutboxFailedEvent(Long outboxId) {}
