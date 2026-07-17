package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

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
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String CONFIRM_REJECTED = "PG가 결제 확정을 거절했습니다.";
    private static final String CONFIRM_RETRYABLE = "PG 결제 확정 요청을 재시도해야 합니다.";
    private static final String CONFIRM_ERROR = "PG 결제 확정 호출 중 오류가 발생했습니다.";
    private static final String REFUND_REJECTED = "PG가 환불을 거절했습니다.";
    private static final String REFUND_RETRYABLE = "PG 환불 요청을 재시도해야 합니다.";
    private static final String REFUND_RESULT_UNKNOWN = "PG 통신 결과를 확인할 수 없습니다.";

    private final RestClient restClient;
    private final String authorizationHeader;

    TossPaymentsProvider(RestClient tossPaymentsRestClient, TossPaymentsProperties properties) {
        this.restClient = tossPaymentsRestClient;
        String encoded = Base64.getEncoder()
                .encodeToString((properties.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
        this.authorizationHeader = "Basic " + encoded;
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        try {
            ConfirmRequest body = new ConfirmRequest(paymentKey, orderId, amount);
            ConfirmResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .header(IDEMPOTENCY_KEY, idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ConfirmResponse.class);
            if (response == null || response.paymentKey() == null) {
                log.warn("Toss confirm: null/invalid response orderId={}", orderId);
                return PaymentConfirmResult.retryableFailure("PG 응답이 비어 있습니다.");
            }
            return PaymentConfirmResult.success(
                    response.paymentKey(),
                    response.method() != null ? response.method() : "UNKNOWN",
                    response.approvedAt());
        } catch (RestClientResponseException e) {
            log.warn("Toss confirm 거절 [orderId={} status={}]", orderId, e.getStatusCode());
            if (isRetryableStatus(e)) {
                return PaymentConfirmResult.retryableFailure(CONFIRM_RETRYABLE);
            }
            return PaymentConfirmResult.failure(CONFIRM_REJECTED);
        } catch (Exception e) {
            log.warn("Toss confirm 예외 [orderId={} type={}]", orderId, e.getClass().getSimpleName());
            return PaymentConfirmResult.retryableFailure(CONFIRM_ERROR);
        }
    }

    @Override
    public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
        try {
            RefundRequest body = new RefundRequest("요청에 의한 환불", amount);
            RefundResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .header(IDEMPOTENCY_KEY, idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RefundResponse.class);
            String refundTransactionKey = refundTransactionKey(response);
            if (refundTransactionKey == null) {
                log.warn("Toss refund 응답에 refund transactionKey가 없습니다.");
                return RefundResult.reconciliationRequired("PG 환불 거래 키가 비어 있어 상태 확인이 필요합니다.");
            }
            return RefundResult.success(refundTransactionKey);
        } catch (RestClientResponseException e) {
            log.warn("Toss refund 거절 [status={}]", e.getStatusCode());
            if (isRetryableStatus(e)) {
                return RefundResult.retryableFailure(REFUND_RETRYABLE);
            }
            return RefundResult.failure(REFUND_REJECTED);
        } catch (ResourceAccessException e) {
            log.warn("Toss refund 통신 결과 불명 [type={}]", e.getClass().getSimpleName());
            return RefundResult.reconciliationRequired(REFUND_RESULT_UNKNOWN);
        } catch (Exception e) {
            log.warn("Toss refund 예외 [type={}]", e.getClass().getSimpleName());
            return RefundResult.reconciliationRequired(REFUND_RESULT_UNKNOWN);
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, long amount) {}

    private record ConfirmResponse(String paymentKey, String orderId, String method, String approvedAt) {}

    private record RefundRequest(String cancelReason, long cancelAmount) {}

    private String refundTransactionKey(RefundResponse response) {
        if (response == null) {
            return null;
        }
        if (StringUtils.hasText(response.lastTransactionKey())) {
            return response.lastTransactionKey();
        }
        if (response.cancels() == null || response.cancels().isEmpty()) {
            return null;
        }
        for (CancelResponse cancel : response.cancels().reversed()) {
            String transactionKey = cancel.transactionKey();
            if (StringUtils.hasText(transactionKey)) {
                return transactionKey;
            }
        }
        return null;
    }

    private boolean isRetryableStatus(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return status == 408 || status == 409 || status == 429
                || exception.getStatusCode().is5xxServerError();
    }

    private record RefundResponse(String paymentKey, String lastTransactionKey, List<CancelResponse> cancels) {}

    private record CancelResponse(String transactionKey) {}
}
