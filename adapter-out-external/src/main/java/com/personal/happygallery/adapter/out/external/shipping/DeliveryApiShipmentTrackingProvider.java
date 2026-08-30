package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DeliveryApiShipmentTrackingProvider implements ShipmentTrackingProvider {

    private static final Logger log = LoggerFactory.getLogger(DeliveryApiShipmentTrackingProvider.class);

    private final RestClient restClient;
    private final DeliveryApiProperties properties;

    public DeliveryApiShipmentTrackingProvider(
            RestClient deliveryApiRestClient,
            DeliveryApiProperties properties) {
        this.restClient = deliveryApiRestClient;
        this.properties = properties;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public RegistrationResult register(RegistrationCommand command) {
        if (!properties.enabled()) {
            return RegistrationResult.retryableFailure("배송조회 연동이 비활성 상태입니다.");
        }
        try {
            RegistrationResponse response = restClient.post()
                    .uri("/v1/webhooks/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegistrationRequest(
                            properties.webhookEndpointId(),
                            true,
                            List.of(new RegistrationItem(
                                    command.carrier().providerCode(),
                                    command.trackingNumber(),
                                    "order-" + command.orderId()))))
                    .retrieve()
                    .body(RegistrationResponse.class);
            if (response == null
                    || !response.isSuccess()
                    || response.data() == null
                    || !StringUtils.hasText(response.data().requestId())) {
                return RegistrationResult.retryableFailure("배송조회 등록 응답이 비어 있습니다.");
            }
            return RegistrationResult.success(response.data().requestId());
        } catch (RestClientResponseException exception) {
            log.warn("배송조회 등록 실패 [orderId={} status={}]",
                    command.orderId(), exception.getStatusCode());
            return exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 429
                    ? RegistrationResult.retryableFailure("배송조회 서비스가 요청을 처리하지 못했습니다.")
                    : RegistrationResult.failure("배송조회 등록 요청이 거절되었습니다.");
        } catch (Exception exception) {
            log.warn("배송조회 등록 통신 실패 [orderId={} type={}]",
                    command.orderId(), exception.getClass().getSimpleName());
            return RegistrationResult.retryableFailure("배송조회 서비스에 연결하지 못했습니다.");
        }
    }

    private record RegistrationRequest(
            String endpointId,
            boolean recurring,
            List<RegistrationItem> items
    ) {}

    private record RegistrationItem(
            String courierCode,
            String trackingNumber,
            String clientId
    ) {}

    private record RegistrationResponse(boolean isSuccess, RegistrationData data) {}

    private record RegistrationData(String requestId) {}
}
