package com.personal.happygallery.domain.notification;

import com.personal.happygallery.domain.text.DomainText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 알림 발송 이력 — notification_log 테이블 */
@Entity
@Table(name = "notification_log")
public class NotificationLog {

    private static final int FAIL_REASON_LIMIT = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "guest_id")
    private Long guestId;

    @Column(name = "channel", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    @Column(name = "event_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private NotificationLogStatus status;

    @Column(name = "fail_reason", length = FAIL_REASON_LIMIT)
    private String failReason;

    @Column(name = "provider_request_id", length = 100)
    private String providerRequestId;

    @Column(name = "provider_recipient_seq")
    private Long providerRecipientSeq;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    protected NotificationLog() {}

    public static NotificationLog success(Long guestId, Long userId,
                                          NotificationChannel channel,
                                          NotificationEventType eventType,
                                          LocalDateTime sentAt) {
        NotificationLog log = new NotificationLog();
        log.guestId = guestId;
        log.userId = userId;
        log.channel = channel;
        log.eventType = eventType;
        log.status = NotificationLogStatus.SUCCESS;
        log.sentAt = sentAt;
        return log;
    }

    public static NotificationLog failed(Long guestId, Long userId,
                                         NotificationChannel channel,
                                         NotificationEventType eventType,
                                         String failReason,
                                         LocalDateTime sentAt) {
        NotificationLog log = new NotificationLog();
        log.guestId = guestId;
        log.userId = userId;
        log.channel = channel;
        log.eventType = eventType;
        log.status = NotificationLogStatus.FAILED;
        log.failReason = failReason;
        log.sentAt = sentAt;
        return log;
    }

    public static NotificationLog requested(
            Long guestId,
            Long userId,
            NotificationChannel channel,
            NotificationEventType eventType,
            String providerRequestId,
            Long providerRecipientSeq,
            LocalDateTime requestedAt) {
        NotificationLog log = new NotificationLog();
        log.guestId = guestId;
        log.userId = userId;
        log.channel = channel;
        log.eventType = eventType;
        log.status = NotificationLogStatus.REQUESTED;
        log.providerRequestId = providerRequestId;
        log.providerRecipientSeq = providerRecipientSeq;
        log.sentAt = requestedAt;
        return log;
    }

    public void markDelivered(LocalDateTime deliveredAt) {
        status = NotificationLogStatus.SUCCESS;
        failReason = null;
        sentAt = deliveredAt;
    }

    public void markFailed(String reason, LocalDateTime failedAt) {
        status = NotificationLogStatus.FAILED;
        failReason = DomainText.truncate(reason, FAIL_REASON_LIMIT);
        sentAt = failedAt;
    }

    public Long getId() { return id; }
    public Long getGuestId() { return guestId; }
    public Long getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationEventType getEventType() { return eventType; }
    public NotificationLogStatus getStatus() { return status; }
    public String getFailReason() { return failReason; }
    public String getProviderRequestId() { return providerRequestId; }
    public Long getProviderRecipientSeq() { return providerRecipientSeq; }
    public LocalDateTime getSentAt() { return sentAt; }
}
