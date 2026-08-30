package com.personal.happygallery.adapter.out.external.address;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JusoRoadAddressSearchProviderTest {

    @DisplayName("도로명주소 검색 응답을 배송지 선택 항목으로 변환한다")
    @Test
    void search_mapsOfficialAddressResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RoadAddressProperties properties = properties();
        JusoRoadAddressSearchProvider provider = new JusoRoadAddressSearchProvider(
                properties,
                builder.baseUrl(properties.baseUrl()).build());
        server.expect(requestTo(containsString("/addrlink/addrLinkApi.do")))
                .andRespond(withSuccess("""
                        {
                          "results": {
                            "common": {"errorCode": "0"},
                            "juso": [{
                              "zipNo": "27360",
                              "roadAddr": "충청북도 충주시 계명대로 161",
                              "jibunAddr": "충청북도 충주시 연수동 1615",
                              "bdNm": "해피갤러리"
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = provider.search("계명대로 161").orElseThrow();

        assertThat(result).singleElement().satisfies(address -> {
            assertThat(address.postalCode()).isEqualTo("27360");
            assertThat(address.roadAddress()).isEqualTo("충청북도 충주시 계명대로 161");
        });
        server.verify();
    }

    private static RoadAddressProperties properties() {
        return new RoadAddressProperties(
                true,
                "confirmation-key",
                "https://business.juso.go.kr",
                Duration.ofSeconds(3),
                Duration.ofSeconds(1),
                Duration.ofMillis(500),
                10,
                Duration.ofSeconds(30));
    }
}
