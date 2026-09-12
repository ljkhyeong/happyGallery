package com.personal.happygallery.adapter.in.web.security.bot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BotProtectionResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
                description = "Turnstile 공개 키. 자동 입력 방지가 꺼져 있으면 null") String siteKey) {}
