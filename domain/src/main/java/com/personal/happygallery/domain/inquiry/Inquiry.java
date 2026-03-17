package com.personal.happygallery.domain.inquiry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "inquiry")
public class Inquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "replied_by")
    private Long repliedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Inquiry() {}

    public Inquiry(Long userId, String title, String content) {
        this.userId = userId;
        this.title = title;
        this.content = content;
    }

    public void reply(String replyContent, Long adminId, LocalDateTime now) {
        if (this.replyContent != null) {
            throw new IllegalStateException("이미 답변이 등록된 문의입니다.");
        }
        this.replyContent = replyContent;
        this.repliedBy = adminId;
        this.repliedAt = now;
    }

    public boolean hasReply() {
        return replyContent != null;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getReplyContent() { return replyContent; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public Long getRepliedBy() { return repliedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
