package com.personal.happygallery.adapter.out.external.address;

import com.personal.happygallery.application.address.port.in.RoadAddressSearchUseCase.RoadAddress;
import com.personal.happygallery.application.address.port.out.RoadAddressSearchProvider;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class JusoRoadAddressSearchProvider implements RoadAddressSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(JusoRoadAddressSearchProvider.class);
    private static final String SUCCESS_CODE = "0";

    private final RoadAddressProperties properties;
    private final RestClient restClient;

    JusoRoadAddressSearchProvider(
            RoadAddressProperties properties,
            RestClient roadAddressRestClient) {
        this.properties = properties;
        this.restClient = roadAddressRestClient;
    }

    @Override
    public Optional<List<RoadAddress>> search(String keyword) {
        if (!properties.enabled()) {
            return Optional.empty();
        }

        try {
            Response response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/addrlink/addrLinkApi.do")
                            .queryParam("confmKey", properties.confirmationKey())
                            .queryParam("currentPage", 1)
                            .queryParam("countPerPage", 10)
                            .queryParam("keyword", keyword)
                            .queryParam("resultType", "json")
                            .build())
                    .retrieve()
                    .body(Response.class);

            if (response == null || response.results() == null || response.results().common() == null
                    || !SUCCESS_CODE.equals(response.results().common().errorCode())) {
                log.warn("도로명주소 검색 응답이 올바르지 않습니다.");
                return Optional.empty();
            }

            List<Juso> addresses = response.results().juso() == null
                    ? List.of()
                    : response.results().juso();
            return Optional.of(addresses.stream()
                    .map(address -> new RoadAddress(
                            address.zipNo(),
                            address.roadAddr(),
                            address.jibunAddr(),
                            address.bdNm()))
                    .toList());
        } catch (RestClientException exception) {
            log.warn("도로명주소 검색 호출에 실패했습니다. type={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private record Response(Results results) {}

    private record Results(Common common, List<Juso> juso) {}

    private record Common(String errorCode) {}

    private record Juso(String zipNo, String roadAddr, String jibunAddr, String bdNm) {}
}
