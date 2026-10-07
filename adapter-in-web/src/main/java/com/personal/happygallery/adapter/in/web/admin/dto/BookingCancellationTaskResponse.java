package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.TaskView;
import com.personal.happygallery.domain.booking.BookingCancellationTaskStatus;
import com.personal.happygallery.domain.booking.BookingCancellationTaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record BookingCancellationTaskResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long taskId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long bookingId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String bookingNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingCancellationTaskType type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingCancellationTaskStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String className,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime startAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long balanceAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long compensationAmount,
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
                task.type(),
                task.status(),
                task.className(),
                task.startAt(),
                task.balanceAmount(),
                task.compensationAmount(),
                task.reason(),
                task.createdAt(),
                task.completedByAdminId(),
                task.completedAt());
    }
}
