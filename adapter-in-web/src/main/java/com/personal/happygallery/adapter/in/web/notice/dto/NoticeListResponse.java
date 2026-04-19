package com.personal.happygallery.adapter.in.web.notice.dto;

import com.personal.happygallery.domain.notice.Notice;
import java.time.LocalDateTime;

public record NoticeListResponse(
        Long id, String title, boolean pinned, int viewCount, LocalDateTime createdAt
) {
    public static NoticeListResponse from(Notice n) {
        return new NoticeListResponse(n.getId(), n.getTitle(), n.isPinned(), n.getViewCount(), n.getCreatedAt());
    }
}
