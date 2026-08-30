package com.personal.happygallery.domain.inquiry;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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

    @Column(nullable = false, length = ContentTextPolicy.MAX_TITLE_LENGTH)
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
        this.title = ContentTextPolicy.requireTitle(title, "문의 제목");
        this.content = ContentTextPolicy.requireBody(content, "문의 내용");
    }

    public void reply(String replyContent, Long adminId, LocalDateTime repliedAt) {
        if (this.replyContent != null) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "이미 답변이 등록된 문의입니다.");
        }
        this.replyContent = ContentTextPolicy.requireBody(replyContent, "문의 답변");
        this.repliedBy = adminId;
        this.repliedAt = repliedAt;
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
