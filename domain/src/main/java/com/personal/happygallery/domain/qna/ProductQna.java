package com.personal.happygallery.domain.qna;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_qna")
public class ProductQna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean secret;

    @Column(name = "password_hash", length = 60)
    private String passwordHash;

    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "replied_by")
    private Long repliedBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ProductQna() {}

    public ProductQna(Long productId, Long userId, String title, String content,
                      boolean secret, String passwordHash) {
        this.productId = productId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.secret = secret;
        this.passwordHash = passwordHash;
    }

    public void reply(String replyContent, Long adminId, LocalDateTime now) {
        if (this.replyContent != null) {
            throw new IllegalStateException("이미 답변이 등록된 Q&A입니다.");
        }
        this.replyContent = replyContent;
        this.repliedBy = adminId;
        this.repliedAt = now;
    }

    public boolean hasReply() {
        return replyContent != null;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isSecret() { return secret; }
    public String getPasswordHash() { return passwordHash; }
    public String getReplyContent() { return replyContent; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public Long getRepliedBy() { return repliedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
