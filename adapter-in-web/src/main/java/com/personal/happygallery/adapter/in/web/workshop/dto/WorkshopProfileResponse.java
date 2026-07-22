package com.personal.happygallery.adapter.in.web.workshop.dto;

import com.personal.happygallery.domain.store.WorkshopProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record WorkshopProfileResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String phone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String postalCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String addressLine1,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String addressLine2,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String businessHours,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "uri") String mapUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String parkingInfo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String businessRegistrationNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String representativeName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String email,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String mailOrderRegistrationNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String introduction,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String kakaoTalkId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "uri") String naverTalkUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "uri") String naverBlogUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "uri") String instagramUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, format = "uri") String smartStoreUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime updatedAt
) {
    public static WorkshopProfileResponse from(WorkshopProfile profile) {
        return new WorkshopProfileResponse(
                profile.getName(),
                profile.getPhone(),
                profile.getPostalCode(),
                profile.getAddressLine1(),
                profile.getAddressLine2(),
                profile.getBusinessHours(),
                profile.getMapUrl(),
                profile.getParkingInfo(),
                profile.getBusinessRegistrationNumber(),
                profile.getRepresentativeName(),
                profile.getEmail(),
                profile.getMailOrderRegistrationNumber(),
                profile.getIntroduction(),
                profile.getKakaoTalkId(),
                profile.getNaverTalkUrl(),
                profile.getNaverBlogUrl(),
                profile.getInstagramUrl(),
                profile.getSmartStoreUrl(),
                profile.getUpdatedAt());
    }
}
