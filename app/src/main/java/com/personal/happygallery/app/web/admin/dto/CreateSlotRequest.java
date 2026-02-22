package com.personal.happygallery.app.web.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateSlotRequest(
        @NotNull Long classId,
        @NotNull LocalDateTime startAt,
        @NotNull LocalDateTime endAt
) {}
