package com.personal.happygallery.adapter.in.web.monitoring.dto;

import com.personal.happygallery.application.monitoring.ClientMonitoringEventType;
import jakarta.validation.constraints.NotNull;

public record CaptureClientEventRequest(@NotNull ClientMonitoringEventType event) {}
