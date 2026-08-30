package com.personal.happygallery.adapter.in.web.address.dto;

import com.personal.happygallery.application.address.port.in.RoadAddressSearchUseCase.RoadAddress;
import io.swagger.v3.oas.annotations.media.Schema;

public record RoadAddressResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String postalCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String roadAddress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String jibunAddress,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String buildingName
) {
    public static RoadAddressResponse from(RoadAddress address) {
        return new RoadAddressResponse(
                address.postalCode(),
                address.roadAddress(),
                address.jibunAddress(),
                address.buildingName());
    }
}
