package com.personal.happygallery.adapter.in.web.webhook;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personal.happygallery.adapter.in.web.GlobalExceptionHandler;
import com.personal.happygallery.application.payment.port.in.PaymentWebhookUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TossPaymentWebhookControllerTest {

    private final PaymentWebhookUseCase useCase = mock(PaymentWebhookUseCase.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new TossPaymentWebhookController(useCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("빈 전송 ID 헤더는 500이 아니라 400 INVALID_INPUT으로 거절하고 웹훅을 접수하지 않는다")
    void receive_blankTransmissionId_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/toss-payments")
                        .header("tosspayments-webhook-transmission-id", " ")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"eventType":"PAYMENT_STATUS_CHANGED","data":{"orderId":"pay_1"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        verify(useCase, never()).receive(any(), any(), any());
    }
}
