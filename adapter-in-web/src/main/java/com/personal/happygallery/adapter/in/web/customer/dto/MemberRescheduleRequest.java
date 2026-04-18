package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotNull;

public record MemberRescheduleRequest(@NotNull Long newSlotId) {}
