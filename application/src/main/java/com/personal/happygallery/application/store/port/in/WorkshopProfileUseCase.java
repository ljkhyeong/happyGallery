package com.personal.happygallery.application.store.port.in;

import com.personal.happygallery.domain.store.WorkshopProfile;

public interface WorkshopProfileUseCase {

    record UpdateCommand(
            String name,
            String phone,
            String postalCode,
            String addressLine1,
            String addressLine2,
            String businessHours,
            String mapUrl,
            String parkingInfo
    ) {}

    WorkshopProfile get();

    WorkshopProfile update(UpdateCommand command);
}
