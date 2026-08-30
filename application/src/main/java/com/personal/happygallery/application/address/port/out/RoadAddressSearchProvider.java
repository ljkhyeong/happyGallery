package com.personal.happygallery.application.address.port.out;

import com.personal.happygallery.application.address.port.in.RoadAddressSearchUseCase.RoadAddress;
import java.util.List;
import java.util.Optional;

public interface RoadAddressSearchProvider {

    Optional<List<RoadAddress>> search(String keyword);
}
