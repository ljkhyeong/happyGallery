package com.personal.happygallery.application.address.port.in;

import java.util.List;

public interface RoadAddressSearchUseCase {

    List<RoadAddress> search(String keyword);

    record RoadAddress(
            String postalCode,
            String roadAddress,
            String jibunAddress,
            String buildingName
    ) {}
}
