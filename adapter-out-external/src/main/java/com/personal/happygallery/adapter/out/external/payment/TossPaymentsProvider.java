package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * Toss Payments 실결제 어댑터 — {@code paymentProviderDelegate} (prod).
 *
 * <p>{@code /v1/payments/confirm}으로 결제 확정, {@code /v1/payments/{paymentKey}/cancel}로 환불.
 * Basic 인증은 {@link TossPaymentsRestClientConfig}가 구성하고, 보호 계층(서킷 브레이커·타임아웃)은
 * {@link ResilientPaymentProvider}가 상위에서 적용한다.
 */
@Component("paymentProviderDelegate")
@Profile("prod")
public class TossPaymentsProvider implements PaymentPort {

    private static final Logger log = LoggerFactory.getLogger(TossPaymentsProvider.class);
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String CONFIRM_REJECTED = "PG가 결제 확정을 거절했습니다.";
    private static final String CONFIRM_RETRYABLE = "PG 결제 확정 요청을 재시도해야 합니다.";
    private static final String CONFIRM_ERROR = "PG 결제 확정 호출 중 오류가 발생했습니다.";
    private static final String CONFIRM_IDENTITY_MISMATCH = "PG 결제 확정 응답 식별자가 요청과 일치하지 않습니다.";
    private static final String REFUND_REJECTED = "PG가 환불을 거절했습니다.";
    private static final String REFUND_RETRYABLE = "PG 환불 요청을 재시도해야 합니다.";
    private static final String REFUND_RESULT_UNKNOWN = "PG 통신 결과를 확인할 수 없습니다.";
    private static final String REFUND_LOOKUP_UNAVAILABLE = "PG 환불 조회 결과를 확인할 수 없습니다.";
    private static final String REFUND_REASON_PREFIX = "해피갤러리 환불:";
    private static final String NOT_FOUND_PAYMENT_CODE = "NOT_FOUND_PAYMENT";
    private static final String CANCEL_DONE = "DONE";

    private final RestClient restClient;

    TossPaymentsProvider(RestClient tossPaymentsRestClient) {
        this.restClient = tossPaymentsRestClient;
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        try {
            ConfirmRequest body = new ConfirmRequest(paymentKey, orderId, amount);
            ConfirmResponse response = restClient.post()
                    .uri("/v1/payments/confirm")
                    .header(IDEMPOTENCY_KEY, idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ConfirmResponse.class);
            if (response == null || !StringUtils.hasText(response.paymentKey())) {
                log.warn("Toss confirm: null/invalid response orderId={}", orderId);
                return PaymentConfirmResult.retryableFailure("PG 응답이 비어 있습니다.");
            }
            if (!paymentKey.equals(response.paymentKey()) || !orderId.equals(response.orderId())) {
                log.warn("Toss confirm 응답 식별자 불일치 [requestOrderId={} responseOrderId={}]",
                        orderId, response.orderId());
                return PaymentConfirmResult.reconciliationRequired(CONFIRM_IDENTITY_MISMATCH);
            }
            return PaymentConfirmResult.success(
                    response.paymentKey(),
                    Objects.requireNonNullElse(response.method(), "UNKNOWN"),
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
    public PaymentLookupResult lookupByOrderId(String orderId) {
        try {
            LookupResponse response = restClient.get()
                    .uri("/v1/payments/orders/{orderId}", orderId)
                    .retrieve()
                    .body(LookupResponse.class);
            if (response == null || !StringUtils.hasText(response.status())) {
                return PaymentLookupResult.unavailable(orderId, "PG 결제 조회 응답이 비어 있습니다.");
            }
            if (!orderId.equals(response.orderId())) {
                return PaymentLookupResult.unavailable(orderId, "PG 결제 조회 식별자가 일치하지 않습니다.");
            }
            return switch (response.status()) {
                case "DONE" -> PaymentLookupResult.approved(
                        response.paymentKey(), response.orderId(), response.totalAmount());
                case "CANCELED", "ABORTED", "EXPIRED" -> PaymentLookupResult.notApproved(
                        orderId, "PG 결제가 승인되지 않았거나 이미 전액 취소되었습니다.");
                default -> PaymentLookupResult.reviewRequired(
                        orderId, "PG 결제 상태를 자동 확정할 수 없습니다: " + response.status());
            };
        } catch (RestClientResponseException e) {
            if (isPaymentNotFound(e)) {
                return PaymentLookupResult.notApproved(orderId, "PG에서 승인된 결제를 찾지 못했습니다.");
            }
            log.warn("Toss 결제 조회 실패 [orderId={} status={}]", orderId, e.getStatusCode());
            return PaymentLookupResult.unavailable(orderId, "PG 결제 조회에 실패했습니다.");
        } catch (Exception e) {
            log.warn("Toss 결제 조회 예외 [orderId={} type={}]", orderId, e.getClass().getSimpleName());
            return PaymentLookupResult.unavailable(orderId, "PG 결제 조회 결과를 확인할 수 없습니다.");
        }
    }

    @Override
    public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
        try {
            String expectedReason = cancelReason(idempotencyKey);
            RefundRequest body = new RefundRequest(expectedReason, amount);
            RefundResponse response = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .header(IDEMPOTENCY_KEY, idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(RefundResponse.class);
            RefundLookupResult lookup = resolveRefund(response, paymentKey, amount, expectedReason);
            if (lookup.status() == RefundLookupResult.Status.REFUNDED) {
                return RefundResult.success(lookup.refundTransactionKey());
            }
            log.warn("Toss refund 응답을 환불 완료로 확정할 수 없습니다. [status={}]", lookup.status());
            return RefundResult.reconciliationRequired(lookup.reason());
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

    @Override
    public RefundLookupResult lookupRefund(String paymentKey, long amount, String idempotencyKey) {
        try {
            RefundResponse response = restClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .retrieve()
                    .body(RefundResponse.class);
            return resolveRefund(response, paymentKey, amount, cancelReason(idempotencyKey));
        } catch (RestClientResponseException e) {
            log.warn("Toss 환불 조회 실패 [status={}]", e.getStatusCode());
            return RefundLookupResult.unavailable(paymentKey, REFUND_LOOKUP_UNAVAILABLE);
        } catch (Exception e) {
            log.warn("Toss 환불 조회 예외 [type={}]", e.getClass().getSimpleName());
            return RefundLookupResult.unavailable(paymentKey, REFUND_LOOKUP_UNAVAILABLE);
        }
    }

    private record ConfirmRequest(String paymentKey, String orderId, long amount) {}

    private record ConfirmResponse(String paymentKey, String orderId, String method, String approvedAt) {}

    private record LookupResponse(String paymentKey, String orderId, String status, long totalAmount) {}

    private record RefundRequest(String cancelReason, long cancelAmount) {}

    private RefundLookupResult resolveRefund(
            RefundResponse response, String paymentKey, long amount, String expectedReason) {
        if (response == null) {
            return RefundLookupResult.unavailable(paymentKey, "PG 환불 조회 응답이 비어 있습니다.");
        }
        if (!paymentKey.equals(response.paymentKey())) {
            return RefundLookupResult.reviewRequired(paymentKey, "PG 환불 조회 식별자가 일치하지 않습니다.");
        }

        CancelResponse cancel = findCancel(response.cancels(), expectedReason);
        if (cancel == null) {
            if (isClearlyNotRefundedPaymentStatus(response.status())) {
                return RefundLookupResult.notRefunded(
                        paymentKey, "PG에 해당 환불 요청의 취소 이력이 없습니다.");
            }
            return RefundLookupResult.reviewRequired(
                    paymentKey, "PG 결제 상태와 환불 이력의 상태 확인이 필요합니다: " + response.status());
        }
        if (cancel.cancelAmount() != amount) {
            return RefundLookupResult.reviewRequired(
                    paymentKey, "PG 취소 금액이 저장된 환불 요청과 일치하지 않습니다.");
        }
        if (!CANCEL_DONE.equals(cancel.cancelStatus()) || !StringUtils.hasText(cancel.transactionKey())) {
            return RefundLookupResult.reviewRequired(
                    paymentKey, "해당 PG 취소가 아직 완료 상태가 아닙니다.");
        }
        if (!isCanceledPaymentStatus(response.status())) {
            return RefundLookupResult.reviewRequired(
                    paymentKey, "PG 취소 이력과 결제 상태가 일치하지 않습니다.");
        }
        return RefundLookupResult.refunded(paymentKey, cancel.cancelAmount(), cancel.transactionKey());
    }

    private CancelResponse findCancel(List<CancelResponse> cancels, String expectedReason) {
        if (cancels == null) {
            return null;
        }
        return cancels.reversed().stream()
                .filter(cancel -> expectedReason.equals(cancel.cancelReason()))
                .findFirst()
                .orElse(null);
    }

    private String cancelReason(String idempotencyKey) {
        return REFUND_REASON_PREFIX + idempotencyKey;
    }

    private boolean isClearlyNotRefundedPaymentStatus(String status) {
        return "DONE".equals(status) || "PARTIAL_CANCELED".equals(status);
    }

    private boolean isCanceledPaymentStatus(String status) {
        return "CANCELED".equals(status) || "PARTIAL_CANCELED".equals(status);
    }

    private boolean isRetryableStatus(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        return status.isSameCodeAs(REQUEST_TIMEOUT)
                || status.isSameCodeAs(CONFLICT)
                || status.isSameCodeAs(TOO_MANY_REQUESTS)
                || status.is5xxServerError();
    }

    private boolean isPaymentNotFound(RestClientResponseException exception) {
        if (!exception.getStatusCode().isSameCodeAs(NOT_FOUND)) {
            return false;
        }
        try {
            TossErrorResponse response = exception.getResponseBodyAs(TossErrorResponse.class);
            return response != null && NOT_FOUND_PAYMENT_CODE.equals(response.code());
        } catch (RuntimeException parsingFailure) {
            log.warn("Toss 결제 조회 404 응답 본문을 해석하지 못했습니다. [type={}]",
                    parsingFailure.getClass().getSimpleName());
            return false;
        }
    }

    private record RefundResponse(
            String paymentKey,
            String status,
            List<CancelResponse> cancels) {}

    private record CancelResponse(
            String transactionKey,
            long cancelAmount,
            String cancelReason,
            String cancelStatus) {}

    private record TossErrorResponse(String code, String message) {}
}
