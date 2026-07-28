package com.personal.happygallery.adapter.in.web.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQnaRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean secret
) {}
