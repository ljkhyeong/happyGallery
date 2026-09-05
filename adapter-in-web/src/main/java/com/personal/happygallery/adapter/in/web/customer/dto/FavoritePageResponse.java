package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.FavoriteUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record FavoritePageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<FavoriteResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore) {
    public record FavoriteResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) FavoriteTargetType targetType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long targetId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean active,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt) {}
    public static FavoritePageResponse from(CursorPage<FavoriteUseCase.View> page) {
        return new FavoritePageResponse(page.content().stream().map(row -> new FavoriteResponse(
                row.id(), row.targetType(), row.targetId(), row.name(), row.active(), row.createdAt())).toList(), page.nextCursor(), page.hasMore());
    }
}
