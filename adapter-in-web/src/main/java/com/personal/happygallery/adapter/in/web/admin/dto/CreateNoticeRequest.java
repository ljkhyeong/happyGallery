package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateNoticeRequest(
        @NotBlank String title,
        @NotBlank String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean pinned
) {}
