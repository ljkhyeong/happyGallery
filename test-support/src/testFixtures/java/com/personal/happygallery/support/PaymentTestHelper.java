package com.personal.happygallery.support;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.payment.PaymentContext;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 결제 prepare/confirm 통합 테스트 헬퍼. */
public final class PaymentTestHelper {

    public static final String PAYMENT_STATUS_TOKEN_HEADER = "X-Payment-Status-Token";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public PaymentTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public record PreparedPayment(String orderId, long amount, String statusToken) {}

    public record ConfirmedPayment(Long domainId, String accessToken) {}

    public PreparedPayment preparePayment(PaymentContext context,
                                          PaymentPayload payload,
                                          Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/payments/prepare")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        PaymentRequestFixtures.prepareRequest(context, payload)));
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new PreparedPayment(
                JsonPath.read(response, "$.orderId"),
                ((Number) JsonPath.read(response, "$.amount")).longValue(),
                JsonPath.read(response, "$.statusToken"));
    }

    public ConfirmedPayment confirmPayment(PreparedPayment prepared,
                                           String paymentKey,
                                           Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/payments/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ConfirmPaymentRequest(
                                paymentKey, prepared.orderId(), prepared.amount())));
        if (prepared.statusToken() != null) {
            request.header(PAYMENT_STATUS_TOKEN_HEADER, prepared.statusToken());
        }
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        String response = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new ConfirmedPayment(
                ((Number) JsonPath.read(response, "$.domainId")).longValue(),
                JsonPath.read(response, "$.accessToken"));
    }

    public ConfirmedPayment createMemberPassBooking(Cookie sessionCookie,
                                                    Long userId,
                                                    Long slotId,
                                                    Long passId) throws Exception {
        PreparedPayment prepared = preparePayment(
                PaymentContext.BOOKING,
                new BookingPayload(userId, null, null, null, slotId, passId, null),
                sessionCookie);
        assertThat(prepared.amount()).isZero();
        return confirmPayment(prepared, null, sessionCookie);
    }

    public ConfirmedPayment createGuestBooking(String phone,
                                               String verificationCode,
                                               String name,
                                               Long slotId,
                                               int participantCount) throws Exception {
        PreparedPayment prepared = preparePayment(
                PaymentContext.BOOKING,
                new BookingPayload(
                        null,
                        phone,
                        verificationCode,
                        name,
                        slotId,
                        null,
                        DepositPaymentMethod.CARD,
                        participantCount,
                        acceptedPolicies()));
        return confirmPayment(prepared, "test-payment-key");
    }

    private PolicyAcceptance acceptedPolicies() {
        return new PolicyAcceptance("2026-08-08-v1", true, "2026-08-11-v2", true);
    }
}
