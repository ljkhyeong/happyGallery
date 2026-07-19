package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateBookingArrearsRequest(
        @NotNull Boolean arrears
) {
}
