package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public record BulkSlotRequest(
        @NotNull Long classId,
        @NotNull LocalDate dateFrom,
        @NotNull LocalDate dateTo,
        @NotEmpty @Size(max = 7) Set<@NotNull DayOfWeek> weekdays,
        @NotEmpty @Size(max = 24) Set<@NotNull LocalTime> startTimes
) {}
