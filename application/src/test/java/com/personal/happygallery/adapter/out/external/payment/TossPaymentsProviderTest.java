package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TossPaymentsProviderTest {

    @DisplayName("Toss 결제 확정은 Basic 인증과 서버 금액으로 confirm 요청을 보낸다")
    @Test
    void confirm_sendsBasicAuthAndAmount_returnsSuccess() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build(), properties("test_secret"));

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth("test_secret")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "paymentKey": "payment-key",
                          "orderId": "order-id",
                          "amount": 10000
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "orderId": "order-id",
                          "method": "카드",
                          "approvedAt": "2026-04-23T10:00:00+09:00"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentConfirmResult result = provider.confirm("payment-key", "order-id", 10_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isTrue();
            softly.assertThat(result.pgRef()).isEqualTo("payment-key");
            softly.assertThat(result.method()).isEqualTo("카드");
            softly.assertThat(result.approvedAt()).isEqualTo("2026-04-23T10:00:00+09:00");
            softly.assertThat(result.failReason()).isNull();
        });
    }

    @DisplayName("Toss 결제 확정 실패 응답은 실패 결과로 변환된다")
    @Test
    void confirm_tossFailureResponse_returnsFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build(), properties("test_secret"));

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "INVALID_REQUEST",
                                  "message": "잘못된 요청입니다."
                                }
                                """));

        PaymentConfirmResult result = provider.confirm("payment-key", "order-id", 10_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.pgRef()).isNull();
            softly.assertThat(result.failReason()).isNotBlank();
        });
    }

    @DisplayName("Toss 환불은 Basic 인증과 취소 금액으로 cancel 요청을 보낸다")
    @Test
    void refund_sendsBasicAuthAndAmount_returnsSuccess() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build(), properties("test_secret"));

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth("test_secret")))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "cancelReason": "요청에 의한 환불",
                          "cancelAmount": 5000
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key"
                        }
                        """, MediaType.APPLICATION_JSON));

        RefundResult result = provider.refund("payment-key", 5_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isTrue();
            softly.assertThat(result.pgRef()).isEqualTo("payment-key");
            softly.assertThat(result.failReason()).isNull();
        });
    }

    @DisplayName("Toss 환불 실패 응답은 실패 결과로 변환된다")
    @Test
    void refund_tossFailureResponse_returnsFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.tosspayments.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build(), properties("test_secret"));

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "INVALID_REFUND",
                                  "message": "환불할 수 없는 결제입니다."
                                }
                                """));

        RefundResult result = provider.refund("payment-key", 5_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.pgRef()).isNull();
            softly.assertThat(result.failReason()).isNotBlank();
        });
    }

    private static TossPaymentsProperties properties(String secretKey) {
        return new TossPaymentsProperties(
                secretKey,
                "https://api.tosspayments.com",
                5_000,
                2_000,
                1_000,
                10,
                30_000);
    }

    private static String basicAuth(String secretKey) {
        String encoded = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
