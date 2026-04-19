package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateClassRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String category,
        @Positive int durationMin,
        @Positive long price,
        @PositiveOrZero int bufferMin
) {
}
