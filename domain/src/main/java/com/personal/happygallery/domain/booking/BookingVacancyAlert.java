package com.personal.happygallery.domain.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_vacancy_alerts")
public class BookingVacancyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Column(name = "guest_id")
    private Long guestId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "access_token_hash", length = 64)
    private String accessTokenHash;

    @Column(name = "active_key", length = 100, unique = true)
    private String activeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VacancyAlertStatus status;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BookingVacancyAlert() {}

    private BookingVacancyAlert(
            Slot slot,
            Long guestId,
            Long userId,
            String accessTokenHash,
            String activeKey
    ) {
        this.slot = slot;
        this.guestId = guestId;
        this.userId = userId;
        this.accessTokenHash = accessTokenHash;
        this.activeKey = activeKey;
        this.status = VacancyAlertStatus.WAITING;
    }

    public static BookingVacancyAlert forGuest(Slot slot, Long guestId, String accessTokenHash) {
        if (guestId == null || accessTokenHash == null || accessTokenHash.isBlank()) {
            throw new IllegalArgumentException("비회원 빈자리 알림 수신자 정보가 올바르지 않습니다.");
        }
        return new BookingVacancyAlert(
                slot,
                guestId,
                null,
                accessTokenHash,
                activeKey(slot.getId(), "GUEST", guestId));
    }

    public static BookingVacancyAlert forUser(Slot slot, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("회원 빈자리 알림 수신자 정보가 올바르지 않습니다.");
        }
        return new BookingVacancyAlert(
                slot,
                null,
                userId,
                null,
                activeKey(slot.getId(), "USER", userId));
    }

    private static String activeKey(Long slotId, String ownerType, Long ownerId) {
        if (slotId == null) {
            throw new IllegalArgumentException("저장된 회차에만 빈자리 알림을 등록할 수 있습니다.");
        }
        return "SLOT:" + slotId + ":" + ownerType + ":" + ownerId;
    }

    public void rotateAccessToken(String accessTokenHash) {
        if (status != VacancyAlertStatus.WAITING
                || accessTokenHash == null
                || accessTokenHash.isBlank()) {
            throw new IllegalStateException("대기 중인 비회원 빈자리 알림만 접근 토큰을 갱신할 수 있습니다.");
        }
        this.accessTokenHash = accessTokenHash;
    }

    public void markNotified(LocalDateTime now) {
        if (status != VacancyAlertStatus.WAITING) return;
        this.status = VacancyAlertStatus.NOTIFIED;
        this.notifiedAt = now;
        this.activeKey = null;
    }

    public void cancel(LocalDateTime now) {
        if (status != VacancyAlertStatus.WAITING) return;
        this.status = VacancyAlertStatus.CANCELED;
        this.canceledAt = now;
        this.activeKey = null;
    }

    public Long getId() { return id; }
    public Slot getSlot() { return slot; }
    public Long getGuestId() { return guestId; }
    public Long getUserId() { return userId; }
    public String getAccessTokenHash() { return accessTokenHash; }
    public VacancyAlertStatus getStatus() { return status; }
    public LocalDateTime getNotifiedAt() { return notifiedAt; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
