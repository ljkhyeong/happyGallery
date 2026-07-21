package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.store.WorkshopProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkshopProfileRequest(
        @NotBlank @Size(max = WorkshopProfile.MAX_NAME_LENGTH) String name,
        @Size(max = WorkshopProfile.MAX_PHONE_LENGTH) String phone,
        @Size(max = WorkshopProfile.MAX_POSTAL_CODE_LENGTH) String postalCode,
        @Size(max = WorkshopProfile.MAX_ADDRESS_LENGTH) String addressLine1,
        @Size(max = WorkshopProfile.MAX_ADDRESS_LENGTH) String addressLine2,
        @Size(max = WorkshopProfile.MAX_BUSINESS_HOURS_LENGTH) String businessHours,
        @Size(max = WorkshopProfile.MAX_MAP_URL_LENGTH) String mapUrl,
        @Size(max = WorkshopProfile.MAX_PARKING_INFO_LENGTH) String parkingInfo,
        @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자등록번호는 000-00-00000 형식이어야 합니다.")
        String businessRegistrationNumber,
        @Size(max = WorkshopProfile.MAX_REPRESENTATIVE_NAME_LENGTH) String representativeName,
        @Email @Size(max = WorkshopProfile.MAX_EMAIL_LENGTH) String email,
        @Size(max = WorkshopProfile.MAX_MAIL_ORDER_REGISTRATION_NUMBER_LENGTH)
        String mailOrderRegistrationNumber,
        @Size(max = WorkshopProfile.MAX_INTRODUCTION_LENGTH) String introduction,
        @Size(max = WorkshopProfile.MAX_KAKAO_TALK_ID_LENGTH) String kakaoTalkId,
        boolean naverTalkEnabled
) {}
