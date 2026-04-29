package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Toss Payments 실결제 어댑터 — {@code paymentProviderDelegate} (prod).
 *
 * <p>{@code /v1/payments/confirm}으로 결제 확정, {@code /v1/payments/{paymentKey}/cancel}로 환불.
 * 인증은 Basic Auth (secretKey + ":" base64 인코딩). 보호 계층(서킷 브레이커·타임아웃)은
 * {@link ResilientPaymentProvider}가 상위에서 씌운다.
 */
@Component("paymentProviderDelegate")
@Profile("prod")
public class TossPaymentsProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsProvider.class);

    private final RestClient restClient;
    private final String authorizationHeader;

    TossPaymentsProvider(RestClient tossPaymentsRestClient, TossPaymentsProperties properties) {
        this.restClient = tossPaymentsRestClient;
        String encoded = Base64.getEncoder()
                .encodeToString((properties.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
        this.authorizationHeader = "Basic " + encoded;
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
        try {
            ConfirmRequest body = new ConfirmRequest(paymentKey, orderId, amount);
            ConfirmResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ConfirmResponse.class);
            if (response == null || response.paymentKey() == null) {
                log.warn("Toss confirm: null/invalid response orderId={}", orderId);
                return PaymentConfirmResult.failure("PG 응답이 비어 있습니다.");
            }
            return PaymentConfirmResult.success(
                    response.paymentKey(),
                    response.method() != null ? response.method() : "UNKNOWN",
                    response.approvedAt());
        } catch (Exception e) {
            log.warn("Toss confirm 예외 orderId={}", orderId, e);
            return PaymentConfirmResult.failure(
                    e.getMessage() != null ? e.getMessage() : "PG 호출 중 오류");
        }
    }

    @Override
    public RefundResult refund(String pgRef, long amount) {
        try {
            RefundRequest body = new RefundRequest("요청에 의한 환불", amount);
            RefundResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", pgRef)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RefundResponse.class);
            if (response == null || response.paymentKey() == null) {
                log.warn("Toss refund: null response pgRef={}", pgRef);
                return RefundResult.failure("PG 응답이 비어 있습니다.");
            }
            return RefundResult.success(response.paymentKey());
        } catch (Exception e) {
            log.warn("Toss refund 예외 pgRef={}", pgRef, e);
            return RefundResult.failure(
                    e.getMessage() != null ? e.getMessage() : "PG 호출 중 오류");
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, long amount) {}

    private record ConfirmResponse(String paymentKey, String orderId, String method, String approvedAt) {}

    private record RefundRequest(String cancelReason, long cancelAmount) {}

    private record RefundResponse(String paymentKey) {}
}
