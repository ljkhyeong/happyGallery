package com.personal.happygallery.domain.notification;

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

    @Column(name = "status", nullable = false, length = 10)
    private String status;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

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
        log.status = "SUCCESS";
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
        log.status = "FAILED";
        log.failReason = failReason;
        log.sentAt = sentAt;
        return log;
    }

    public void markRead(LocalDateTime now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public Long getId() { return id; }
    public Long getGuestId() { return guestId; }
    public Long getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationEventType getEventType() { return eventType; }
    public String getStatus() { return status; }
    public String getFailReason() { return failReason; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
