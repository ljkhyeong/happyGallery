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
        if (version != expectedVersion) throw new HappyGalleryException(ErrorCode.CONFLICT, "다른 관리자가 상담을 변경했습니다. 새로고침 후 다시 확인해 주세요.");
        status.requireTransitionTo(next);
        status = next;
        if (next == GroupInquiryStatus.CLOSED) nextContactOn = null;
        updatedAt = now.isAfter(updatedAt) ? now : updatedAt.plusNanos(1000);
    }

    public void scheduleContact(long expectedVersion, LocalDate date, LocalDateTime now) {
        if (status == GroupInquiryStatus.CLOSED) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "종료한 상담은 다시 연 뒤 연락일을 지정해 주세요.");
        }
        recordConsultation(expectedVersion, status, now);
        nextContactOn = date;
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
