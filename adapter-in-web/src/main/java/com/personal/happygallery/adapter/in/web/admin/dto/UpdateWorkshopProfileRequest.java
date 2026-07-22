package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.store.WorkshopProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkshopProfileRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        @NotBlank @Size(min = 1, max = WorkshopProfile.MAX_NAME_LENGTH) String name,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_PHONE_LENGTH) String phone,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_POSTAL_CODE_LENGTH) String postalCode,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_ADDRESS_LENGTH) String addressLine1,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_ADDRESS_LENGTH) String addressLine2,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_BUSINESS_HOURS_LENGTH) String businessHours,
        @Schema(nullable = true, format = "uri", pattern = "^[hH][tT][tT][pP][sS]?://.+")
        @Size(max = WorkshopProfile.MAX_URL_LENGTH) String mapUrl,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_PARKING_INFO_LENGTH) String parkingInfo,
        @Schema(nullable = true)
        @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호는 000-00-00000 형식이어야 합니다.")
        String businessRegistrationNumber,
        @Schema(nullable = true)
        @Size(max = WorkshopProfile.MAX_REPRESENTATIVE_NAME_LENGTH) String representativeName,
        @Schema(nullable = true) @Email @Size(max = WorkshopProfile.MAX_EMAIL_LENGTH) String email,
        @Schema(nullable = true)
        @Size(max = WorkshopProfile.MAX_MAIL_ORDER_REGISTRATION_NUMBER_LENGTH)
        String mailOrderRegistrationNumber,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_INTRODUCTION_LENGTH) String introduction,
        @Schema(nullable = true) @Size(max = WorkshopProfile.MAX_KAKAO_TALK_ID_LENGTH) String kakaoTalkId,
        @Schema(nullable = true, format = "uri", pattern = "^[hH][tT][tT][pP][sS]?://.+")
        @Size(max = WorkshopProfile.MAX_URL_LENGTH) String naverTalkUrl,
        @Schema(nullable = true, format = "uri", pattern = "^[hH][tT][tT][pP][sS]?://.+")
        @Size(max = WorkshopProfile.MAX_URL_LENGTH) String naverBlogUrl,
        @Schema(nullable = true, format = "uri", pattern = "^[hH][tT][tT][pP][sS]?://.+")
        @Size(max = WorkshopProfile.MAX_URL_LENGTH) String instagramUrl,
        @Schema(nullable = true, format = "uri", pattern = "^[hH][tT][tT][pP][sS]?://.+")
        @Size(max = WorkshopProfile.MAX_URL_LENGTH) String smartStoreUrl
) {}
