package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotItem;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotResult;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record BulkSlotResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long creatableCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long createdCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long skippedCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<BulkSlotItemResponse> items
) {
    public static BulkSlotResponse from(BulkSlotResult result) {
        return new BulkSlotResponse(
                result.items().size(),
                result.creatableCount(),
                result.createdCount(),
                result.skippedCount(),
                result.items().stream().map(BulkSlotItemResponse::from).toList());
    }

    public record BulkSlotItemResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long slotId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime endAt,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BulkSlotStatus status,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean bufferBlocked
    ) {
        private static BulkSlotItemResponse from(BulkSlotItem item) {
            return new BulkSlotItemResponse(
                    item.slotId(),
                    item.startAt(),
                    item.endAt(),
                    item.status(),
                    item.bufferBlocked());
        }
    }
}
