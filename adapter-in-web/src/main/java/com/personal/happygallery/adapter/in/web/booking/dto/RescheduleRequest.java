package com.personal.happygallery.adapter.in.web.booking.dto;

import jakarta.validation.constraints.NotNull;

public record RescheduleRequest(
        @NotNull Long newSlotId
) {}
