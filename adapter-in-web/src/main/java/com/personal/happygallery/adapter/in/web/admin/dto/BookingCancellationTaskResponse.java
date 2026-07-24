package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.TaskView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BookingCancellationTaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long taskId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"BALANCE_SETTLEMENT", "MANUAL_COMPENSATION"})
        String type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"PENDING", "COMPLETED"})
        String status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long completedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime completedAt
) {
    public static BookingCancellationTaskResponse from(TaskView task) {
        return new BookingCancellationTaskResponse(
                task.taskId(),
                task.bookingId(),
                task.bookingNumber(),
                task.type().name(),
                task.status().name(),
                task.className(),
                task.startAt(),
                task.balanceAmount(),
                task.reason(),
                task.createdAt(),
                task.completedByAdminId(),
                task.completedAt());
    }
}
