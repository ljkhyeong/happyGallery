package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** PATCH /api/v1/admin/orders/{id}/expected-ship-date 요청 바디 */
public record SetExpectedShipDateRequest(
        @Schema(nullable = true) LocalDate expectedShipDate
) {}
