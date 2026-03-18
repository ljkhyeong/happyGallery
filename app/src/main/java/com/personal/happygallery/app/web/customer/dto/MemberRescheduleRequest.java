package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.NotNull;

public record MemberRescheduleRequest(@NotNull Long newSlotId) {}
