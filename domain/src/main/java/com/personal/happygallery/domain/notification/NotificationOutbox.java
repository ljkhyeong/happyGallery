package com.personal.happygallery.domain.notification;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForGuest;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent.ForUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutbox {

    private static final int LAST_ERROR_LIMIT = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 10)
    private NotificationRecipientType recipientType;

    @Column(name = "guest_id")
    private Long guestId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(name = "aggregate_type", length = 40)
    private String aggregateType;

    @Column(name = "aggregate_id")
    private Long aggregateId;

    @Column(name = "idempotency_key", nullable = false, length = 200, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error", length = LAST_ERROR_LIMIT)
    private String lastError;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NotificationOutbox() {}

    private NotificationOutbox(NotificationRecipientType recipientType,
                               Long guestId,
                               Long userId,
                               NotificationEventType eventType,
                               String aggregateType,
                               Long aggregateId,
                               String idempotencyKey,
                               LocalDateTime now) {
        this.recipientType = recipientType;
        this.guestId = guestId;
        this.userId = userId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.idempotencyKey = idempotencyKey;
        this.status = NotificationOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
    }

    public static NotificationOutbox from(NotificationRequestedEvent event, LocalDateTime now) {
        return switch (event) {
            case ForGuest e -> forGuest(e.guestId(), e.eventType(), e.aggregateType(), e.aggregateId(),
                    e.idempotencyKey(), now);
            case ForUser e -> forUser(e.userId(), e.eventType(), e.aggregateType(), e.aggregateId(),
                    e.idempotencyKey(), now);
        };
    }

    private static NotificationOutbox forGuest(Long guestId,
                                               NotificationEventType eventType,
                                               String aggregateType,
                                               Long aggregateId,
                                               String idempotencyKey,
                                               LocalDateTime now) {
        return new NotificationOutbox(
                NotificationRecipientType.GUEST,
                guestId,
                null,
                eventType,
                aggregateType,
                aggregateId,
                idempotencyKey,
                now);
    }

    private static NotificationOutbox forUser(Long userId,
                                              NotificationEventType eventType,
                                              String aggregateType,
                                              Long aggregateId,
                                              String idempotencyKey,
                                              LocalDateTime now) {
        return new NotificationOutbox(
                NotificationRecipientType.USER,
                null,
                userId,
                eventType,
                aggregateType,
                aggregateId,
                idempotencyKey,
                now);
    }

    public void markProcessing(LocalDateTime now) {
        this.status = NotificationOutboxStatus.PROCESSING;
        this.lockedAt = now;
        this.lastError = null;
    }

    public void markSent(LocalDateTime now) {
        this.status = NotificationOutboxStatus.SENT;
        this.processedAt = now;
        this.lockedAt = null;
        this.lastError = null;
    }

    public boolean markDeliveryFailed(String reason,
                                      LocalDateTime nextAttemptAt,
                                      LocalDateTime now,
                                      int maxAttempts) {
        this.attemptCount++;
        this.lastError = truncate(reason);
        this.lockedAt = null;
        if (this.attemptCount >= maxAttempts) {
            this.status = NotificationOutboxStatus.FAILED;
            this.processedAt = now;
            return true;
        }
        this.status = NotificationOutboxStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        return false;
    }

    /** 운영자가 최종 실패를 확인한 뒤 같은 outbox와 멱등키로 다시 발송하도록 연다. */
    public void retryFailed(LocalDateTime now) {
        if (status != NotificationOutboxStatus.FAILED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "최종 실패한 알림만 재처리할 수 있습니다.");
        }
        this.status = NotificationOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.lockedAt = null;
        this.processedAt = null;
        this.lastError = null;
    }

    private String truncate(String reason) {
        if (reason == null || reason.length() <= LAST_ERROR_LIMIT) {
            return reason;
        }
        return reason.substring(0, LAST_ERROR_LIMIT);
    }

    public Long getId() { return id; }
    public NotificationRecipientType getRecipientType() { return recipientType; }
    public Long getGuestId() { return guestId; }
    public Long getUserId() { return userId; }
    public NotificationEventType getEventType() { return eventType; }
    public String getAggregateType() { return aggregateType; }
    public Long getAggregateId() { return aggregateId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public NotificationOutboxStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public LocalDateTime getLockedAt() { return lockedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public String getLastError() { return lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
