package com.personal.happygallery.adapter.in.web.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SocialAccountAuthorizationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String authorizationUrl) {}
