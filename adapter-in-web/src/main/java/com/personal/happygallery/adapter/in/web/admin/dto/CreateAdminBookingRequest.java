package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.booking.port.in.AdminBookingCreateUseCase.CreateAdminBookingCommand;
import com.personal.happygallery.domain.booking.BookingSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAdminBookingRequest(
        @NotNull @Positive
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long slotId,
        @NotBlank @Size(min = 1, max = 100)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
        @NotBlank @Size(min = 1, max = 20)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,
        @Min(1)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        int participantCount,
        @NotNull
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"PHONE", "NAVER_TALK", "KAKAO", "VISIT"})
        BookingSource source,
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean depositPaid
) {

    public CreateAdminBookingCommand toCommand(Long adminId) {
        return new CreateAdminBookingCommand(
                slotId,
                name,
                phone,
                participantCount,
                source,
                depositPaid,
                adminId);
    }
}
