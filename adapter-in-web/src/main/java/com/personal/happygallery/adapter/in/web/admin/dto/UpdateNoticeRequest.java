package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateNoticeRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotBlank String title,
        @NotBlank String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean pinned
) {}
