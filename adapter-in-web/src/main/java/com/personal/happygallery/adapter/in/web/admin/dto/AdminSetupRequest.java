package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSetupRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Size(min = 10, max = 100) String password
) {}
