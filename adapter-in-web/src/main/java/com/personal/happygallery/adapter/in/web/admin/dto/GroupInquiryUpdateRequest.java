package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record GroupInquiryUpdateRequest(@NotNull @PositiveOrZero Long version,
        @NotNull GroupInquiryStatus status, @NotBlank @Size(max = 2000) String note) {}
