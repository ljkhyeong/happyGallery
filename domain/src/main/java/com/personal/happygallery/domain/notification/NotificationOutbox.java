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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

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

    @Column(name = "processing_token", length = 64)
    private String processingToken;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "last_error", length = LAST_ERROR_LIMIT)
    private String lastError;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    @Column(nullable = false)
    private long version;

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

    public String markProcessing(LocalDateTime now) {
        if (status != NotificationOutboxStatus.PENDING) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "대기 중인 알림만 처음 선점할 수 있습니다.");
        }
        return beginProcessing(now);
    }

    public String reclaimProcessing(LocalDateTime now, LocalDateTime staleBefore) {
        if (status != NotificationOutboxStatus.PROCESSING
                || lockedAt == null
                || !lockedAt.isBefore(staleBefore)) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "처리 시간이 만료된 알림만 다시 선점할 수 있습니다.");
        }
        return beginProcessing(now);
    }

    private String beginProcessing(LocalDateTime now) {
        this.status = NotificationOutboxStatus.PROCESSING;
        this.lockedAt = now;
        this.processingToken = UUID.randomUUID().toString();
        this.lastError = null;
        return processingToken;
    }

    /** 발송 준비 시점의 현재 aggregate 소유자로 수신자를 갱신하고 실행권 lease를 연장한다. */
    public boolean refreshRecipient(String token,
                                    NotificationRecipientType recipientType,
                                    Long guestId,
                                    Long userId,
                                    LocalDateTime now) {
        if (!isProcessingOwnedBy(token)) {
            return false;
        }
        switch (recipientType) {
            case GUEST -> {
                if (guestId == null || userId != null) {
                    throw new IllegalArgumentException("비회원 알림 수신자 정보가 올바르지 않습니다.");
                }
                this.recipientType = NotificationRecipientType.GUEST;
                this.guestId = guestId;
                this.userId = null;
            }
            case USER -> {
                if (userId == null || guestId != null) {
                    throw new IllegalArgumentException("회원 알림 수신자 정보가 올바르지 않습니다.");
                }
                this.recipientType = NotificationRecipientType.USER;
                this.guestId = null;
                this.userId = userId;
            }
        }
        this.lockedAt = now;
        return true;
    }

    public boolean markSent(String token, LocalDateTime now) {
        return completeSent(token, now, null);
    }

    public boolean markSentWithAuditFailure(String token, LocalDateTime now, String reason) {
        return completeSent(token, now, truncate(reason));
    }

    private boolean completeSent(String token, LocalDateTime now, String lastError) {
        if (!isProcessingOwnedBy(token)) {
            return false;
        }
        this.status = NotificationOutboxStatus.SENT;
        this.processedAt = now;
        this.lastError = lastError;
        clearProcessing();
        return true;
    }

    /** 발송 직전 현재 도메인 상태를 다시 확인해 의미가 사라진 알림을 종결한다. */
    public boolean markObsolete(String token, LocalDateTime now, String reason) {
        if (!isProcessingOwnedBy(token)) {
            return false;
        }
        this.status = NotificationOutboxStatus.OBSOLETE;
        this.processedAt = now;
        this.lastError = truncate(reason);
        clearProcessing();
        return true;
    }

    /** 변경된 일정이 이후 유효 구간에 다시 들어오면 배치가 같은 멱등 행을 재사용한다. */
    public boolean reactivateObsolete(NotificationRequestedEvent event, LocalDateTime now) {
        if (status != NotificationOutboxStatus.OBSOLETE) {
            return false;
        }
        if (!eventType.isTimeSensitiveReminder()
                || !idempotencyKey.equals(event.idempotencyKey())
                || eventType != event.eventType()
                || !Objects.equals(aggregateType, event.aggregateType())
                || !Objects.equals(aggregateId, event.aggregateId())) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "같은 리마인드 요청만 다시 열 수 있습니다.");
        }
        switch (event) {
            case ForGuest e -> {
                this.recipientType = NotificationRecipientType.GUEST;
                this.guestId = e.guestId();
                this.userId = null;
            }
            case ForUser e -> {
                this.recipientType = NotificationRecipientType.USER;
                this.guestId = null;
                this.userId = e.userId();
            }
        }
        this.status = NotificationOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        this.processedAt = null;
        this.readAt = null;
        this.lastError = null;
        clearProcessing();
        return true;
    }

    public boolean markDeliveryFailed(String token,
                                      String reason,
                                      LocalDateTime nextAttemptAt,
                                      LocalDateTime now,
                                      int maxAttempts) {
        if (!isProcessingOwnedBy(token)) {
            return false;
        }
        this.attemptCount++;
        this.lastError = truncate(reason);
        clearProcessing();
        if (this.attemptCount >= maxAttempts) {
            this.status = NotificationOutboxStatus.FAILED;
            this.processedAt = now;
            return true;
        }
        this.status = NotificationOutboxStatus.PENDING;
        this.nextAttemptAt = nextAttemptAt;
        return true;
    }

    public boolean isProcessingOwnedBy(String token) {
        return status == NotificationOutboxStatus.PROCESSING
                && token != null
                && token.equals(processingToken);
    }

    /** 운영자가 최종 실패를 확인한 뒤 같은 outbox와 멱등키로 다시 발송하도록 연다. */
    public void retryFailed(LocalDateTime now) {
        if (status != NotificationOutboxStatus.FAILED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "최종 실패한 알림만 재처리할 수 있습니다.");
        }
        this.status = NotificationOutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextAttemptAt = now;
        clearProcessing();
        this.processedAt = null;
        this.lastError = null;
    }

    public void markRead(LocalDateTime now) {
        if (status != NotificationOutboxStatus.SENT) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "발송이 완료된 알림만 읽음 처리할 수 있습니다.");
        }
        if (readAt == null) {
            readAt = now;
        }
    }

    private void clearProcessing() {
        this.lockedAt = null;
        this.processingToken = null;
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
    public String getProcessingToken() { return processingToken; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public boolean isRead() { return readAt != null; }
    public String getLastError() { return lastError; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getVersion() { return version; }
}
