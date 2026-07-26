package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotItem;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotResult;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase.BulkSlotStatus;
import java.time.LocalDateTime;
import java.util.List;

public record BulkSlotResponse(
        int totalCount,
        long creatableCount,
        long createdCount,
        long skippedCount,
        List<BulkSlotItemResponse> items
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
            Long slotId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BulkSlotStatus status,
            boolean bufferBlocked
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
