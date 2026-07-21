package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.store.WorkshopProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWorkshopProfileRequest(
        @NotBlank @Size(max = WorkshopProfile.MAX_NAME_LENGTH) String name,
        @Size(max = WorkshopProfile.MAX_PHONE_LENGTH) String phone,
        @Size(max = WorkshopProfile.MAX_POSTAL_CODE_LENGTH) String postalCode,
        @Size(max = WorkshopProfile.MAX_ADDRESS_LENGTH) String addressLine1,
        @Size(max = WorkshopProfile.MAX_ADDRESS_LENGTH) String addressLine2,
        @Size(max = WorkshopProfile.MAX_BUSINESS_HOURS_LENGTH) String businessHours,
        @Size(max = WorkshopProfile.MAX_MAP_URL_LENGTH) String mapUrl,
        @Size(max = WorkshopProfile.MAX_PARKING_INFO_LENGTH) String parkingInfo
) {}
