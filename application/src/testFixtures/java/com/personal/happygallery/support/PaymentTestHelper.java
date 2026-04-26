package com.personal.happygallery.support;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.assertj.core.api.Assertions;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 결제 prepare/confirm 통합 테스트 헬퍼.
 */
public final class PaymentTestHelper {

    private PaymentTestHelper() {
    }

    public record PreparedPayment(String orderId, long amount, String responseBody) {}

    public record ConfirmedPayment(Long domainId, String accessToken, String responseBody) {}

    public static PreparedPayment preparePayment(MockMvc mockMvc,
                                                 String context,
                                                 String payloadJson,
                                                 Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder request = post("/api/v1/payments/prepare")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "context": "%s",
                                  "payload": %s
                                }
                                """.formatted(context, payloadJson));
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
                response);
    }

    public static ConfirmedPayment confirmPayment(MockMvc mockMvc,
                                                  String orderId,
                                                  long amount,
                                                  String paymentKey,
                                                  Cookie... cookies) throws Exception {
        String paymentKeyJson = paymentKey == null ? "null" : "\"" + paymentKey + "\"";
        MockHttpServletRequestBuilder request = post("/api/v1/payments/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "paymentKey": %s,
                                  "orderId": "%s",
                                  "amount": %d
                                }
                                """.formatted(paymentKeyJson, orderId, amount));
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
                JsonPath.read(response, "$.accessToken"),
                response);
    }

    public static ConfirmedPayment createMemberOrder(MockMvc mockMvc,
                                                     Cookie sessionCookie,
                                                     Long userId,
                                                     Long productId,
                                                     int qty) throws Exception {
        PreparedPayment prepared = preparePayment(mockMvc, "ORDER", """
                {
                  "type": "ORDER",
                  "userId": %d,
                  "items": [
                    { "productId": %d, "qty": %d }
                  ]
                }
                """.formatted(userId, productId, qty), sessionCookie);
        return confirmPayment(mockMvc, prepared.orderId(), prepared.amount(), "test-payment-key", sessionCookie);
    }

    public static ConfirmedPayment createMemberDepositBooking(MockMvc mockMvc,
                                                              Cookie sessionCookie,
                                                              Long userId,
                                                              Long slotId) throws Exception {
        PreparedPayment prepared = preparePayment(mockMvc, "BOOKING", """
                {
                  "type": "BOOKING",
                  "userId": %d,
                  "slotId": %d,
                  "paymentMethod": "CARD"
                }
                """.formatted(userId, slotId), sessionCookie);
        return confirmPayment(mockMvc, prepared.orderId(), prepared.amount(), "test-payment-key", sessionCookie);
    }

    public static ConfirmedPayment createMemberPassBooking(MockMvc mockMvc,
                                                           Cookie sessionCookie,
                                                           Long userId,
                                                           Long slotId,
                                                           Long passId) throws Exception {
        PreparedPayment prepared = preparePayment(mockMvc, "BOOKING", """
                {
                  "type": "BOOKING",
                  "userId": %d,
                  "slotId": %d,
                  "passId": %d
                }
                """.formatted(userId, slotId, passId), sessionCookie);
        Assertions.assertThat(prepared.amount()).isZero();
        return confirmPayment(mockMvc, prepared.orderId(), prepared.amount(), null, sessionCookie);
    }

    public static ConfirmedPayment purchaseMemberPass(MockMvc mockMvc,
                                                      Cookie sessionCookie,
                                                      Long userId) throws Exception {
        PreparedPayment prepared = preparePayment(mockMvc, "PASS", """
                {
                  "type": "PASS",
                  "userId": %d
                }
                """.formatted(userId), sessionCookie);
        return confirmPayment(mockMvc, prepared.orderId(), prepared.amount(), "test-payment-key", sessionCookie);
    }

    public static ConfirmedPayment createGuestBooking(MockMvc mockMvc,
                                                      String phone,
                                                      String verificationCode,
                                                      String name,
                                                      Long slotId) throws Exception {
        PreparedPayment prepared = preparePayment(mockMvc, "BOOKING", """
                {
                  "type": "BOOKING",
                  "phone": "%s",
                  "verificationCode": "%s",
                  "name": "%s",
                  "slotId": %d,
                  "paymentMethod": "CARD"
                }
                """.formatted(phone, verificationCode, name, slotId));
        return confirmPayment(mockMvc, prepared.orderId(), prepared.amount(), "test-payment-key");
    }
}
