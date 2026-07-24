package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
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

    private static final TossPaymentsProperties PROPERTIES = new TossPaymentsProperties(
            "test_secret",
            "https://api.tosspayments.com",
            5_000,
            2_000,
            1_000,
            10,
            30_000);

    @DisplayName("Toss 결제 확정은 Basic 인증과 서버 금액으로 confirm 요청을 보낸다")
    @Test
    void confirm_sendsBasicAuthAndAmount_returnsSuccess() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth(PROPERTIES.secretKey())))
                .andExpect(header("Idempotency-Key", "confirm-idempotency-key"))
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

        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000L, "confirm-idempotency-key");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isTrue();
            softly.assertThat(result.paymentKey()).isEqualTo("payment-key");
            softly.assertThat(result.method()).isEqualTo("카드");
            softly.assertThat(result.approvedAt()).isEqualTo("2026-04-23T10:00:00+09:00");
            softly.assertThat(result.failReason()).isNull();
        });
    }

    @DisplayName("Toss 결제 확정 실패 응답은 실패 결과로 변환된다")
    @Test
    void confirm_tossFailureResponse_returnsFailure() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "INVALID_REQUEST",
                                  "message": "payment-key로 결제를 확정할 수 없습니다."
                                }
                                """));

        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000L, "confirm-idempotency-key");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.paymentKey()).isNull();
            softly.assertThat(result.failReason()).isEqualTo("PG가 결제 확정을 거절했습니다.");
        });
    }

    @DisplayName("Toss 결제 확정 응답 식별자가 요청과 다르면 즉시 대사가 필요한 실패로 처리한다")
    @Test
    void confirm_responseIdentityMismatch_returnsReconciliationRequired() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "different-payment-key",
                          "orderId": "different-order-id",
                          "method": "카드",
                          "approvedAt": "2026-04-23T10:00:00+09:00"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentConfirmResult result = provider.confirm(
                "payment-key", "order-id", 10_000L, "confirm-idempotency-key");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.retryable()).isFalse();
            softly.assertThat(result.reconciliationRequired()).isTrue();
            softly.assertThat(result.failReason()).contains("식별자");
        });
    }

    @DisplayName("Toss 주문번호 결제 조회는 완료된 승인을 대사 결과로 변환한다")
    @Test
    void lookupByOrderId_donePayment_returnsApproved() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/orders/order-id"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth(PROPERTIES.secretKey())))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "orderId": "order-id",
                          "status": "DONE",
                          "totalAmount": 10000
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentLookupResult result = provider.lookupByOrderId("order-id");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(PaymentLookupResult.Status.APPROVED);
            softly.assertThat(result.paymentKey()).isEqualTo("payment-key");
            softly.assertThat(result.orderId()).isEqualTo("order-id");
            softly.assertThat(result.totalAmount()).isEqualTo(10_000L);
        });
    }

    @DisplayName("Toss 결제 조회의 명시적인 결제 미존재 응답만 미승인으로 확정한다")
    @Test
    void lookupByOrderId_paymentNotFound_returnsNotApproved() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/orders/order-id"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "NOT_FOUND_PAYMENT",
                                  "message": "존재하지 않는 결제 정보입니다."
                                }
                                """));

        PaymentLookupResult result = provider.lookupByOrderId("order-id");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(PaymentLookupResult.Status.NOT_APPROVED);
            softly.assertThat(result.orderId()).isEqualTo("order-id");
        });
    }

    @DisplayName("Toss 결제 조회의 다른 404 응답은 결제 미승인으로 확정하지 않는다")
    @Test
    void lookupByOrderId_otherNotFound_returnsUnavailable() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/orders/order-id"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "NOT_FOUND_PAYMENT_SESSION",
                                  "message": "결제 진행 데이터가 존재하지 않습니다."
                                }
                                """));

        PaymentLookupResult result = provider.lookupByOrderId("order-id");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(PaymentLookupResult.Status.UNAVAILABLE);
            softly.assertThat(result.orderId()).isEqualTo("order-id");
        });
    }

    @DisplayName("Toss 환불은 Basic 인증과 취소 금액으로 cancel 요청을 보낸다")
    @Test
    void refund_sendsBasicAuthAndAmount_returnsSuccess() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth(PROPERTIES.secretKey())))
                .andExpect(header("Idempotency-Key", "refund-idempotency-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "cancelReason": "요청에 의한 환불",
                          "cancelAmount": 5000
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "status": "CANCELED",
                          "lastTransactionKey": "refund-transaction-key",
                          "cancels": [
                            {
                              "transactionKey": "refund-transaction-key",
                              "cancelAmount": 5000,
                              "cancelStatus": "DONE"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RefundResult result = provider.refund("payment-key", 5_000L, "refund-idempotency-key");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isTrue();
            softly.assertThat(result.refundTransactionKey()).isEqualTo("refund-transaction-key");
            softly.assertThat(result.failReason()).isNull();
        });
    }

    @DisplayName("Toss 환불 응답에 취소 거래 키가 없으면 상태 확인 필요 결과로 변환된다")
    @Test
    void refund_withoutTransactionKey_returnsFailure() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key"
                        }
                        """, MediaType.APPLICATION_JSON));

        RefundResult result = provider.refund("payment-key", 5_000L, "refund-idempotency-key");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.reconciliationRequired()).isTrue();
            softly.assertThat(result.refundTransactionKey()).isNull();
            softly.assertThat(result.failReason()).contains("상태 확인");
        });
    }

    @DisplayName("Toss 환불 실패 응답은 실패 결과로 변환된다")
    @Test
    void refund_tossFailureResponse_returnsFailure() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "INVALID_REFUND",
                                  "message": "payment-key는 환불할 수 없습니다."
                                }
                                """));

        RefundResult result = provider.refund("payment-key", 5_000L, "refund-idempotency-key");

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.success()).isFalse();
            softly.assertThat(result.retryable()).isFalse();
            softly.assertThat(result.reconciliationRequired()).isFalse();
            softly.assertThat(result.refundTransactionKey()).isNull();
            softly.assertThat(result.failReason()).isEqualTo("PG가 환불을 거절했습니다.");
        });
    }

    @DisplayName("Toss 환불 조회는 완료 상태와 취소 금액이 일치하는 거래를 확정한다")
    @Test
    void lookupRefund_matchingDoneCancel_returnsRefunded() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basicAuth(PROPERTIES.secretKey())))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "status": "PARTIAL_CANCELED",
                          "lastTransactionKey": "refund-transaction-key",
                          "cancels": [
                            {
                              "transactionKey": "refund-transaction-key",
                              "cancelAmount": 5000,
                              "cancelStatus": "DONE"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RefundLookupResult result = provider.lookupRefund("payment-key", 5_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(RefundLookupResult.Status.REFUNDED);
            softly.assertThat(result.paymentKey()).isEqualTo("payment-key");
            softly.assertThat(result.cancelAmount()).isEqualTo(5_000L);
            softly.assertThat(result.refundTransactionKey()).isEqualTo("refund-transaction-key");
        });
    }

    @DisplayName("Toss 환불 조회의 취소 금액이 다르면 완료로 확정하지 않는다")
    @Test
    void lookupRefund_cancelAmountMismatch_returnsReviewRequired() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key"))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "status": "PARTIAL_CANCELED",
                          "lastTransactionKey": "other-refund-transaction-key",
                          "cancels": [
                            {
                              "transactionKey": "other-refund-transaction-key",
                              "cancelAmount": 3000,
                              "cancelStatus": "DONE"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        RefundLookupResult result = provider.lookupRefund("payment-key", 5_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(RefundLookupResult.Status.REVIEW_REQUIRED);
            softly.assertThat(result.refundTransactionKey()).isNull();
            softly.assertThat(result.reason()).contains("금액");
        });
    }

    @DisplayName("Toss 환불 조회에 취소 이력이 없으면 안전한 재호출 가능 상태로 반환한다")
    @Test
    void lookupRefund_withoutCancels_returnsNotRefunded() {
        RestClient.Builder builder = tossRestClientBuilder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TossPaymentsProvider provider = new TossPaymentsProvider(builder.build());

        server.expect(requestTo("https://api.tosspayments.com/v1/payments/payment-key"))
                .andRespond(withSuccess("""
                        {
                          "paymentKey": "payment-key",
                          "status": "DONE",
                          "cancels": []
                        }
                        """, MediaType.APPLICATION_JSON));

        RefundLookupResult result = provider.lookupRefund("payment-key", 5_000L);

        server.verify();
        assertSoftly(softly -> {
            softly.assertThat(result.status()).isEqualTo(RefundLookupResult.Status.NOT_REFUNDED);
            softly.assertThat(result.paymentKey()).isEqualTo("payment-key");
        });
    }

    private static RestClient.Builder tossRestClientBuilder() {
        return TossPaymentsRestClientConfig.configure(RestClient.builder(), PROPERTIES);
    }

    private static String basicAuth(String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(secretKey, "");
        return headers.getFirst(HttpHeaders.AUTHORIZATION);
    }
}
