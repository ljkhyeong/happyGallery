package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record GroupInquiryContactRequest(@NotNull @PositiveOrZero Long version,
        @Schema(nullable = true, description = "서울 날짜. 생략 또는 null이면 연락 예정일 해제") LocalDate nextContactOn) {}
