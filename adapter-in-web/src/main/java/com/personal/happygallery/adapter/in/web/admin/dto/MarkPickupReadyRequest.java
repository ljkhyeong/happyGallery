package com.personal.happygallery.adapter.in.web.admin.dto;

import java.time.LocalDateTime;

/** POST /api/v1/admin/orders/{id}/prepare-pickup 요청 바디 */
public record MarkPickupReadyRequest(LocalDateTime pickupDeadlineAt) {}
