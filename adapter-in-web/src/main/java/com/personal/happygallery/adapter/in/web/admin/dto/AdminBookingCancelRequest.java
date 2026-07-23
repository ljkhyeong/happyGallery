package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminBookingCancelRequest(
        @NotBlank
        @Size(max = 200)
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String reason
) {}
