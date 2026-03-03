package com.personal.happygallery.app.web.admin.dto;

import java.time.LocalDateTime;

/** POST /admin/orders/{id}/pickup-ready 요청 바디 */
public record MarkPickupReadyRequest(LocalDateTime pickupDeadlineAt) {}
