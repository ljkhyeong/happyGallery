package com.personal.happygallery.domain.inquiry;

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
@Table(name = "group_inquiry_activities")
public class GroupInquiryActivity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "inquiry_id", nullable = false)
    private Long inquiryId;
    @Column(name = "admin_id")
    private Long adminId;
    @Column(name = "member_action", nullable = false)
    private boolean memberAction;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 20)
    private GroupInquiryStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 20)
    private GroupInquiryStatus toStatus;
    @Column(name = "note_enc", nullable = false, columnDefinition = "TEXT")
    private String noteEnc;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GroupInquiryActivity() {}

    public GroupInquiryActivity(Long inquiryId, Long adminId, GroupInquiryStatus fromStatus,
            GroupInquiryStatus toStatus, String noteEnc, LocalDateTime now) {
        this.inquiryId = inquiryId;
        this.adminId = adminId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.noteEnc = noteEnc;
        this.createdAt = now;
    }

    public static GroupInquiryActivity forMember(Long inquiryId, GroupInquiryStatus fromStatus,
            GroupInquiryStatus toStatus, String noteEnc, LocalDateTime now) {
        var activity = new GroupInquiryActivity(inquiryId, null, fromStatus, toStatus, noteEnc, now);
        activity.memberAction = true;
        return activity;
    }

    public boolean isMemberAction() { return memberAction; }
    public Long getId() { return id; }
    public Long getAdminId() { return adminId; }
    public GroupInquiryStatus getFromStatus() { return fromStatus; }
    public GroupInquiryStatus getToStatus() { return toStatus; }
    public String getNoteEnc() { return noteEnc; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
