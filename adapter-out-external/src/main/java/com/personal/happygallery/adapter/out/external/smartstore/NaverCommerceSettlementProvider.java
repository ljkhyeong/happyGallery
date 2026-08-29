package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NaverCommerceSettlementProvider implements SmartStoreSettlementProvider {

    private static final int PAGE_SIZE = 1000;
    private static final String PAY_DATE = "SETTLE_CASEBYCASE_PAY_DATE";
    private static final String SETTLED = "SETTLED";

    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final NaverCommerceAccessTokenProvider accessTokenProvider;

    public NaverCommerceSettlementProvider(
            RestClient smartStoreRestClient,
            SmartStoreProperties properties,
            NaverCommerceAccessTokenProvider accessTokenProvider) {
        this.restClient = smartStoreRestClient;
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public List<SettlementItem> findByPayDate(LocalDate payDate) {
        List<SettlementItem> results = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            SettlementResponse response = fetch(payDate, page);
            SettlementPage settlementPage = response.data() == null
                    ? new SettlementPage(response.elements(), response.pagination())
                    : response.data();
            List<SettlementContent> elements = settlementPage.elements() == null
                    ? List.of()
                    : settlementPage.elements();
            elements.stream().map(NaverCommerceSettlementProvider::toItem).forEach(results::add);
            totalPages = settlementPage.pagination() == null
                    ? page
                    : settlementPage.pagination().totalPages();
            page++;
        } while (page <= totalPages);
        return List.copyOf(results);
    }

    private SettlementResponse fetch(LocalDate payDate, int page) {
        SettlementResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/pay-settle/settle/case")
                        .queryParam("searchDate", payDate)
                        .queryParam("periodType", PAY_DATE)
                        .queryParam("settleDecisionType", SETTLED)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(SettlementResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 정산 응답이 비어 있습니다.");
        }
        return response;
    }

    private static SettlementItem toItem(SettlementContent content) {
        return new SettlementItem(
                content.productOrderId(), content.orderId(), content.productOrderType(),
                content.settleType(), content.productName(), amount(content.paySettleAmount()),
                nullableAmount(content.totalPayCommissionAmount()),
                nullableAmount(content.sellingInterlockCommissionAmount()),
                amount(content.benefitSettleAmount()), amount(content.settleExpectAmount()),
                content.settleBasisDate(), content.settleExpectDate(),
                content.settleCompleteDate(), content.payDate());
    }

    private static long amount(BigDecimal value) {
        if (value == null) {
            throw new IllegalStateException("스마트스토어 정산 금액이 비어 있습니다.");
        }
        return value.longValueExact();
    }

    private static Long nullableAmount(BigDecimal value) {
        return value == null ? null : value.longValueExact();
    }

    private record SettlementResponse(
            List<SettlementContent> elements,
            Pagination pagination,
            SettlementPage data
    ) {}

    private record SettlementPage(
            List<SettlementContent> elements,
            Pagination pagination
    ) {}

    private record Pagination(int page, int size, int totalPages, long totalElements) {}

    private record SettlementContent(
            LocalDate settleBasisDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            LocalDate payDate,
            String orderId,
            String productOrderId,
            String productOrderType,
            String settleType,
            String productName,
            BigDecimal paySettleAmount,
            BigDecimal totalPayCommissionAmount,
            BigDecimal sellingInterlockCommissionAmount,
            BigDecimal benefitSettleAmount,
            BigDecimal settleExpectAmount
    ) {}
}
