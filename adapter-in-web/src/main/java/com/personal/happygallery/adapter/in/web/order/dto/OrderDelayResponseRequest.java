package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.domain.order.OrderDelayDecision;
import jakarta.validation.constraints.NotNull;

public record OrderDelayResponseRequest(@NotNull OrderDelayDecision decision) {}
