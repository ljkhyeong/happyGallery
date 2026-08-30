package com.personal.happygallery.adapter.out.external.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationCommand;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DeliveryApiShipmentTrackingProviderTest {

    @Test
    @DisplayName("배송조회 등록 요청과 응답 봉투의 requestId를 변환한다")
    void register_mapsRequestAndResponseEnvelope() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DeliveryApiProperties properties = properties();
        DeliveryApiShipmentTrackingProvider provider = new DeliveryApiShipmentTrackingProvider(
                builder
                        .baseUrl(properties.baseUrl())
                        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer api-key:secret-key")
                        .build(),
                properties);
        server.expect(once(), requestTo("https://api.deliveryapi.co.kr/v1/webhooks/register"))
                .andExpect(request -> assertThat(request.getMethod()).isEqualTo(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer api-key:secret-key"))
                .andExpect(jsonPath("$.endpointId").value("endpoint-id"))
                .andExpect(jsonPath("$.recurring").value(true))
                .andExpect(jsonPath("$.items[0].courierCode").value("cj"))
                .andExpect(jsonPath("$.items[0].clientId").value("order-10"))
                .andRespond(withSuccess("""
                        {
                          "isSuccess": true,
                          "data": {"requestId": "req-1", "itemCount": 1, "recurring": true}
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = provider.register(
                new RegistrationCommand(10L, ShippingCarrier.CJ_LOGISTICS, "123456789012"));

        assertThat(result.success()).isTrue();
        assertThat(result.requestId()).isEqualTo("req-1");
        server.verify();
    }

    private static DeliveryApiProperties properties() {
        return new DeliveryApiProperties(
                true,
                "api-key",
                "secret-key",
                "endpoint-id",
                "webhook-secret",
                "https://api.deliveryapi.co.kr",
                Duration.ofSeconds(3),
                Duration.ofSeconds(1),
                Duration.ofMillis(500),
                10,
                Duration.ofSeconds(30));
    }
}
