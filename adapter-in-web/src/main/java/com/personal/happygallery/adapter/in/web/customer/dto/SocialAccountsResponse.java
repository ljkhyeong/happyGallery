package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.user.SocialProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SocialAccountsResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<SocialProvider> linkedProviders) {}
