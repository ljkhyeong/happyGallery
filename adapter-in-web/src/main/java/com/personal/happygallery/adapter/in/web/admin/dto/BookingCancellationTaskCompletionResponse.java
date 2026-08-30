package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.CompletionResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookingCancellationTaskCompletionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BookingCancellationTaskResponse task,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean changed
) {
    public static BookingCancellationTaskCompletionResponse from(CompletionResult result) {
        return new BookingCancellationTaskCompletionResponse(
                BookingCancellationTaskResponse.from(result.task()),
                result.changed());
    }
}
