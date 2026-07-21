package com.personal.happygallery.adapter.in.web.workshop.dto;

import com.personal.happygallery.domain.store.WorkshopProfile;
import java.time.LocalDateTime;

public record WorkshopProfileResponse(
        String name,
        String phone,
        String postalCode,
        String addressLine1,
        String addressLine2,
        String businessHours,
        String mapUrl,
        String parkingInfo,
        LocalDateTime updatedAt
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
                profile.getUpdatedAt());
    }
}
