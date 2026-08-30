package com.personal.happygallery.adapter.in.web.webhook;

import com.personal.happygallery.adapter.in.web.webhook.dto.TossPaymentWebhookRequest;
import com.personal.happygallery.application.payment.port.in.PaymentWebhookUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/v1/webhooks/toss-payments")
public class TossPaymentWebhookController {

    private static final String TRANSMISSION_ID = "tosspayments-webhook-transmission-id";

    private final PaymentWebhookUseCase useCase;

    public TossPaymentWebhookController(PaymentWebhookUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(operationId = "receiveTossPaymentWebhook", summary = "토스 결제 상태 웹훅 수신")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public void receive(
            @RequestHeader(TRANSMISSION_ID) @NotBlank @Size(max = 100) String transmissionId,
            @Valid @RequestBody TossPaymentWebhookRequest request) {
        useCase.receive(transmissionId, request.eventType(), request.data().orderId());
    }
}
