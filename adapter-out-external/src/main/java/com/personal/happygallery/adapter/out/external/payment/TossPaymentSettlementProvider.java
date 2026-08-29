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
            int pageNumber = page;
            List<SettlementResponse> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/settlements")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .queryParam("dateType", "soldDate")
                            .queryParam("page", pageNumber)
                            .queryParam("size", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (response == null || response.isEmpty()) {
                break;
            }
            response.stream().map(SettlementResponse::toItem).forEach(results::add);
            if (response.size() < PAGE_SIZE) {
                break;
            }
        }
        return results;
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
