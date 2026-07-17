package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminSetupRequest(
        @NotBlank String token,
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "username은 영문, 숫자, '.', '_', '-'만 사용할 수 있습니다.")
        String username,
        @NotBlank @Size(min = 10, max = 100) String password
) {}
