package com.personal.happygallery.application.address;

import com.personal.happygallery.application.address.port.in.RoadAddressSearchUseCase;
import com.personal.happygallery.application.address.port.out.RoadAddressSearchProvider;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultRoadAddressSearchService implements RoadAddressSearchUseCase {

    private final RoadAddressSearchProvider provider;

    public DefaultRoadAddressSearchService(RoadAddressSearchProvider provider) {
        this.provider = provider;
    }

    @Override
    public List<RoadAddress> search(String keyword) {
        return provider.search(keyword.trim())
                .orElseThrow(() -> new HappyGalleryException(
                        ErrorCode.SERVICE_UNAVAILABLE,
                        "주소 검색을 일시적으로 사용할 수 없습니다. 주소를 직접 입력해 주세요."));
    }
}
