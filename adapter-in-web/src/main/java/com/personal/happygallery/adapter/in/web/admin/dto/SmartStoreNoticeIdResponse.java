package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SmartStoreNoticeIdResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long sellerNoticeId
) {}
