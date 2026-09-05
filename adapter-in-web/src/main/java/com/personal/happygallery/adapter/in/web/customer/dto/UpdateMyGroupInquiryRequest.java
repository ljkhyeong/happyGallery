package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateMyGroupInquiryRequest(@NotNull @PositiveOrZero Long version,
        @NotNull @Min(1) @Max(500) Integer headcount,
        @NotBlank @Size(max = 200) String preferredSchedule) {}
