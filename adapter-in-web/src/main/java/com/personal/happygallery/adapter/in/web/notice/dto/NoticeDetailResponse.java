package com.personal.happygallery.adapter.in.web.notice.dto;

import com.personal.happygallery.domain.notice.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NoticeDetailResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean pinned,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int viewCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static NoticeDetailResponse from(Notice n) {
        return new NoticeDetailResponse(
                n.getId(),
                n.getTitle(),
                n.getContent(),
                n.isPinned(),
                n.getViewCount(),
                n.getVersion(),
                n.getCreatedAt());
    }
}
