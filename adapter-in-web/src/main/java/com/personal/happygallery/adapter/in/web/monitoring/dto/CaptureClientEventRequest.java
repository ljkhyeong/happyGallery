package com.personal.happygallery.adapter.in.web.monitoring.dto;

import com.personal.happygallery.application.monitoring.ClientMonitoringEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CaptureClientEventRequest(
        @NotNull ClientMonitoringEventType event,
        @NotBlank @Size(max = 120) String path,
        @Size(max = 80) String source,
        @Size(max = 80) String target) {}
