package com.personal.happygallery.app.web.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateNoticeRequest(
        @NotBlank String title,
        @NotBlank String content,
        boolean pinned
) {}
