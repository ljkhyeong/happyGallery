package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** POST /api/v1/admin/orders/{id}/prepare-pickup 요청 바디 */
public record MarkPickupReadyRequest(
        @Schema(nullable = true) LocalDateTime pickupDeadlineAt
) {}
