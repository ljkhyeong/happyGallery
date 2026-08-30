package com.personal.happygallery.adapter.in.web.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase.TrackingUpdate;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookVerifier;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.ShipmentTrackingStatus;
import com.personal.happygallery.domain.order.ShippingCarrier;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class DeliveryTrackingWebhookControllerTest {

    @Test
    @DisplayName("서명된 배송 웹훅을 주문 배송 상태와 이력 명령으로 변환한다")
    void receive_mapsSignedWebhook() {
        ShipmentTrackingWebhookVerifier verifier = mock(ShipmentTrackingWebhookVerifier.class);
        ShipmentTrackingWebhookUseCase useCase = mock(ShipmentTrackingWebhookUseCase.class);
        DeliveryTrackingWebhookController controller = new DeliveryTrackingWebhookController(
                verifier, useCase, JsonMapper.builder().build());
        byte[] body = """
                {
                  "event":"tracking.completed",
                  "requestId":"req-1",
                  "items":[{
                    "courierCode":"cj",
                    "trackingNumber":"123456789012",
                    "clientId":"order-10",
                    "currentStatus":"DELIVERED",
                    "trackingData":{
                      "deliveryStatusText":"배송완료",
                      "progresses":[{
                        "dateTime":"2026-08-27 15:30:00",
                        "location":"서울 강남",
                        "status":"배송완료",
                        "statusCode":"DELIVERED"
                      }]
                    }
                  }]
                }
                """.getBytes(StandardCharsets.UTF_8);
        when(verifier.verify(eq("1735729200"), eq("sha256=signature"), any())).thenReturn(true);

        controller.receive("1735729200", "sha256=signature", body);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TrackingUpdate>> captor = ArgumentCaptor.forClass(List.class);
        verify(useCase).apply(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(update -> {
            assertThat(update.orderId()).isEqualTo(10L);
            assertThat(update.carrier()).isEqualTo(ShippingCarrier.CJ_LOGISTICS);
            assertThat(update.status()).isEqualTo(ShipmentTrackingStatus.DELIVERED);
            assertThat(update.events()).singleElement().satisfies(event -> {
                assertThat(event.occurredAt()).isEqualTo(LocalDateTime.of(2026, 8, 27, 15, 30));
                assertThat(event.location()).isEqualTo("서울 강남");
            });
        });
    }

    @Test
    @DisplayName("배송 웹훅 서명이 올바르지 않으면 본문을 처리하지 않는다")
    void receive_rejectsInvalidSignature() {
        ShipmentTrackingWebhookVerifier verifier = mock(ShipmentTrackingWebhookVerifier.class);
        ShipmentTrackingWebhookUseCase useCase = mock(ShipmentTrackingWebhookUseCase.class);
        DeliveryTrackingWebhookController controller = new DeliveryTrackingWebhookController(
                verifier, useCase, JsonMapper.builder().build());
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> controller.receive("1735729200", "invalid", body))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
