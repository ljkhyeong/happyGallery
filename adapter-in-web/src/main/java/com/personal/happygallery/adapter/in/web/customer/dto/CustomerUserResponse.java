package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerUserResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String phone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean phoneVerified,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean localPasswordEnabled
) {

    public static CustomerUserResponse from(User user) {
        return new CustomerUserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.hasLocalPassword()
        );
    }
}
