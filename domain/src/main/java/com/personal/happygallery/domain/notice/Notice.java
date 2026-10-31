package com.personal.happygallery.domain.notice;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = ContentTextPolicy.MAX_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "view_count", nullable = false, updatable = false)
    private int viewCount;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Notice() {}

    public Notice(String title, String content, boolean pinned) {
        this.title = ContentTextPolicy.requireTitle(title, "공지 제목");
        this.content = ContentTextPolicy.requireBody(content, "공지 내용");
        this.pinned = pinned;
        this.viewCount = 0;
    }

    public void update(String title, String content, boolean pinned) {
        this.title = ContentTextPolicy.requireTitle(title, "공지 제목");
        this.content = ContentTextPolicy.requireBody(content, "공지 내용");
        this.pinned = pinned;
    }

    /** 불러온 뒤 다른 관리자가 먼저 수정했으면 충돌로 거절한다. */
    public void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "다른 관리자가 공지사항을 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 처리해주세요.");
        }
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isPinned() { return pinned; }
    public int getViewCount() { return viewCount; }
    public long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
