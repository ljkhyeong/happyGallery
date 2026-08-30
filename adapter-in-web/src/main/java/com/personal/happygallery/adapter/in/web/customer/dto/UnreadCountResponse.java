package com.personal.happygallery.adapter.in.web.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnreadCountResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long count) {}
