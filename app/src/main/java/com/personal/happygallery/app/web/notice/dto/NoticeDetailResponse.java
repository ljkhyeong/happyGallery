package com.personal.happygallery.app.web.notice.dto;

import com.personal.happygallery.domain.notice.Notice;
import java.time.LocalDateTime;

public record NoticeDetailResponse(
        Long id, String title, String content, boolean pinned, int viewCount, LocalDateTime createdAt
) {
    public static NoticeDetailResponse from(Notice n) {
        return new NoticeDetailResponse(
                n.getId(), n.getTitle(), n.getContent(), n.isPinned(), n.getViewCount(), n.getCreatedAt());
    }
}
