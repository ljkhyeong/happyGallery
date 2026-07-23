package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateBookingArrearsRequest(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean arrears
) {
}
