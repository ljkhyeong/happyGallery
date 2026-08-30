package com.personal.happygallery.adapter.in.web.webhook;

import com.personal.happygallery.adapter.in.web.webhook.dto.DeliveryTrackingWebhookRequest;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookUseCase;
import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookVerifier;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/webhooks/delivery-tracking")
public class DeliveryTrackingWebhookController {

    private static final String WEBHOOK_TIMESTAMP = "X-Webhook-Timestamp";
    private static final String WEBHOOK_SIGNATURE = "X-Webhook-Signature";

    private final ShipmentTrackingWebhookVerifier verifier;
    private final ShipmentTrackingWebhookUseCase useCase;
    private final ObjectMapper objectMapper;

    public DeliveryTrackingWebhookController(
            ShipmentTrackingWebhookVerifier verifier,
            ShipmentTrackingWebhookUseCase useCase,
            ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    @Operation(operationId = "receiveDeliveryTrackingWebhook", summary = "택배 배송현황 웹훅 수신")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DeliveryTrackingWebhookRequest.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void receive(
            @RequestHeader(WEBHOOK_TIMESTAMP) String timestamp,
            @RequestHeader(WEBHOOK_SIGNATURE) String signature,
            @RequestBody byte[] body) {
        if (!verifier.verify(timestamp, signature, body)) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED, "배송조회 웹훅 서명이 올바르지 않습니다.");
        }
        DeliveryTrackingWebhookRequest request =
                objectMapper.readValue(body, DeliveryTrackingWebhookRequest.class);
        useCase.apply(request.toUpdates());
    }
}
