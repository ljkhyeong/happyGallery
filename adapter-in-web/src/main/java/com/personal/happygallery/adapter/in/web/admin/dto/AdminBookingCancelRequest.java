package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminBookingCancelRequest(
        @NotBlank @Size(max = 200) String reason
) {}
