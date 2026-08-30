package com.personal.happygallery.adapter.in.web.notice.dto;

import com.personal.happygallery.domain.notice.Notice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record NoticeListResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean pinned,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int viewCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static NoticeListResponse from(Notice n) {
        return new NoticeListResponse(
                n.getId(),
                n.getTitle(),
                n.isPinned(),
                n.getViewCount(),
                n.getVersion(),
                n.getCreatedAt());
    }
}
