package com.personal.happygallery.domain.inquiry;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
import java.time.LocalDate;

@Entity
@Table(name = "group_inquiries")
public class GroupInquiry {
    public enum Source { WEBSITE, EXTERNAL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Source source;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private GroupInquiryStatus status;
    @Column(name = "details_enc", nullable = false, columnDefinition = "TEXT")
    private String detailsEnc;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "next_contact_on")
    private LocalDate nextContactOn;
    @Version private long version;

    protected GroupInquiry() {}

    public GroupInquiry(Long userId, Source source, String detailsEnc, LocalDateTime now) {
        this.userId = userId;
        this.source = source;
        this.detailsEnc = detailsEnc;
        this.status = GroupInquiryStatus.RECEIVED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void recordConsultation(long expectedVersion, GroupInquiryStatus next, LocalDateTime now) {
        requireVersion(expectedVersion);
        status.requireTransitionTo(next);
        status = next;
        if (next == GroupInquiryStatus.CLOSED) nextContactOn = null;
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plusNanos(1000);
    }

    public void scheduleContact(long expectedVersion, LocalDate date, LocalDateTime now) {
        if (status == GroupInquiryStatus.CLOSED || status == GroupInquiryStatus.CANCELED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "종료하거나 취소한 문의에는 연락일을 지정할 수 없습니다.");
        }
        recordConsultation(expectedVersion, status, now);
        nextContactOn = date;
    }

    public void reviseByMember(long expectedVersion, String encryptedDetails, LocalDateTime now) {
        requireVersion(expectedVersion);
        status.requireMemberChange();
        detailsEnc = encryptedDetails;
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plusNanos(1000);
    }

    public void cancelByMember(long expectedVersion, LocalDateTime now) {
        requireVersion(expectedVersion);
        status.requireMemberChange();
        status = GroupInquiryStatus.CANCELED;
        nextContactOn = null;
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plusNanos(1000);
    }

    private void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "문의가 변경되었습니다. 최신 내용을 확인한 뒤 다시 저장해 주세요.");
        }
    }

    public LocalDate getNextContactOn() { return nextContactOn; }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Source getSource() { return source; }
    public GroupInquiryStatus getStatus() { return status; }
    public String getDetailsEnc() { return detailsEnc; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
