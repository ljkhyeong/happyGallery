package com.personal.happygallery.support;

import com.jayway.jsonpath.JsonPath;
import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.PaymentContext;
import jakarta.servlet.http.Cookie;
import java.util.List;
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
        return confirmPayment(
                prepared.orderId(), prepared.amount(), paymentKey, prepared.statusToken(), cookies);
    }

    public ConfirmedPayment confirmPayment(String orderId,
                                           long amount,
                                           String paymentKey,
                                           Cookie... cookies) throws Exception {
        return confirmPayment(orderId, amount, paymentKey, null, cookies);
    }

    private ConfirmedPayment confirmPayment(String orderId,
                                            long amount,
                                            String paymentKey,
                                            String statusToken,
                                            Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/payments/confirm")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ConfirmPaymentRequest(paymentKey, orderId, amount)));
        if (statusToken != null) {
            request.header(PAYMENT_STATUS_TOKEN_HEADER, statusToken);
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

    public ConfirmedPayment createMemberOrder(Cookie sessionCookie,
                                              Long userId,
                                              Long productId,
                                              int qty) throws Exception {
        PreparedPayment prepared = preparePayment(
                PaymentContext.ORDER,
                new OrderPayload(userId, null, null, null, List.of(new OrderItemRef(productId, qty))),
                sessionCookie);
        return confirmPayment(prepared, "test-payment-key", sessionCookie);
    }

    public ConfirmedPayment createMemberShippingOrder(
            Cookie sessionCookie,
            Long userId,
            Long productId,
            int qty,
            ShippingAddress shippingAddress
    ) throws Exception {
        PreparedPayment prepared = preparePayment(
                PaymentContext.ORDER,
                new OrderPayload(
                        userId,
                        null,
                        null,
                        null,
                        List.of(new OrderItemRef(productId, qty)),
                        false,
                        FulfillmentType.SHIPPING,
                        shippingAddress),
                sessionCookie);
        return confirmPayment(prepared, "test-payment-key", sessionCookie);
    }

    public ConfirmedPayment createMemberDepositBooking(Cookie sessionCookie,
                                                       Long userId,
                                                       Long slotId) throws Exception {
        PreparedPayment prepared = preparePayment(
                PaymentContext.BOOKING,
                new BookingPayload(userId, null, null, null, slotId, null, DepositPaymentMethod.CARD),
                sessionCookie);
        return confirmPayment(prepared, "test-payment-key", sessionCookie);
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

    public ConfirmedPayment purchaseMemberPass(Cookie sessionCookie, Long userId) throws Exception {
        PreparedPayment prepared = preparePayment(PaymentContext.PASS, new PassPayload(userId), sessionCookie);
        return confirmPayment(prepared, "test-payment-key", sessionCookie);
    }

    public ConfirmedPayment createGuestBooking(String phone,
                                               String verificationCode,
                                               String name,
                                               Long slotId) throws Exception {
        return createGuestBooking(phone, verificationCode, name, slotId, 1);
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
        return new PolicyAcceptance("2026-08-08-v1", true, "2026-08-08-v1", true);
    }
}
