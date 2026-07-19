package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordChangeRequest(
        @NotBlank @Size(max = 100) String currentPassword,
        @NotBlank @Size(min = 10, max = 100) String newPassword
) {}
