package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentSettlementItem;
import com.personal.happygallery.application.payment.port.out.PaymentSettlementProvider;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@Profile("prod")
public class TossPaymentSettlementProvider implements PaymentSettlementProvider {

    private static final int PAGE_SIZE = 5000;

    private final RestClient restClient;

    public TossPaymentSettlementProvider(
            @Qualifier("tossSettlementRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<PaymentSettlementItem> findSettlements(LocalDate startDate, LocalDate endDate) {
        List<PaymentSettlementItem> results = new ArrayList<>();
        for (int page = 1; ; page++) {
            List<SettlementResponse> response = fetchPage(startDate, endDate, page);
            if (response == null) {
                throw new IllegalStateException("토스 정산 응답이 비어 있습니다.");
            }
            if (response.isEmpty()) {
                break;
            }
            response.stream().map(SettlementResponse::toItem).forEach(results::add);
            if (response.size() < PAGE_SIZE) {
                break;
            }
        }
        return results;
    }

    private List<SettlementResponse> fetchPage(LocalDate startDate, LocalDate endDate, int page) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/settlements")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .queryParam("dateType", "soldDate")
                            .queryParam("page", page)
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException exception) {
            // 배치 로그에 외부 응답 원문이 남지 않도록 원인 예외를 연결하지 않는다.
            throw new IllegalStateException(
                    "토스 정산 조회에 실패했습니다. (HTTP " + exception.getStatusCode().value() + ")");
        } catch (RestClientException exception) {
            throw new IllegalStateException("토스 정산 조회 응답을 처리하지 못했습니다. [type="
                    + exception.getClass().getSimpleName() + "]");
        }
    }

    private record SettlementResponse(
            String transactionKey,
            String paymentKey,
            String orderId,
            String method,
            long amount,
            List<Fee> fees,
            long supplyAmount,
            long vat,
            long payOutAmount,
            String approvedAt,
            LocalDate soldDate,
            LocalDate paidOutDate,
            Cancel cancel
    ) {
        private PaymentSettlementItem toItem() {
            long feeAmount = fees == null
                    ? 0L
                    : fees.stream().mapToLong(Fee::fee).sum();
            String providerTransactionKey = cancel != null && cancel.transactionKey() != null
                    ? cancel.transactionKey()
                    : transactionKey;
            long transactionAmount = cancel == null ? amount : cancel.cancelAmount();
            return new PaymentSettlementItem(
                    providerTransactionKey,
                    paymentKey,
                    orderId,
                    method,
                    transactionAmount,
                    feeAmount,
                    supplyAmount,
                    vat,
                    payOutAmount,
                    approvedAt,
                    soldDate,
                    paidOutDate,
                    cancel != null);
        }
    }

    private record Fee(String type, long fee) {}

    private record Cancel(long cancelAmount, String transactionKey) {}
}
