package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.order.port.out.SmartStoreSettlementProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import org.springframework.core.ParameterizedTypeReference;
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
        return readPages(page -> fetch(payDate, page), NaverCommerceSettlementProvider::toItem);
    }

    @Override
    public List<DailySettlement> findDailySettlements(LocalDate from, LocalDate to) {
        return readPages(page -> fetchDailySettlements(from, to, page),
                NaverCommerceSettlementProvider::toDailySettlement);
    }

    @Override
    public List<CommissionDetail> findCommissionDetails(LocalDate from, LocalDate to) {
        List<CommissionDetail> results = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDate payDate = date;
            results.addAll(readPages(page -> fetchCommissions(payDate, page),
                    NaverCommerceSettlementProvider::toCommissionDetail));
        }
        return List.copyOf(results);
    }

    @Override
    public List<DailyVat> findDailyVat(LocalDate from, LocalDate to) {
        return readPages(page -> fetchVat(from, to, page),
                NaverCommerceSettlementProvider::toDailyVat);
    }

    private static <T, R> List<R> readPages(
            IntFunction<PageResponse<T>> fetchPage, Function<T, R> mapper) {
        List<R> results = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            PageResponse<T> response = fetchPage.apply(page);
            Page<T> current = response.data() == null
                    ? new Page<>(response.elements(), response.pagination())
                    : response.data();
            if (current.elements() != null) {
                current.elements().stream().map(mapper).forEach(results::add);
            }
            totalPages = current.pagination() == null ? page : current.pagination().totalPages();
            page++;
        } while (page <= totalPages);
        return List.copyOf(results);
    }

    private PageResponse<SettlementContent> fetch(LocalDate payDate, int page) {
        PageResponse<SettlementContent> response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/pay-settle/settle/case")
                        .queryParam("searchDate", payDate)
                        .queryParam("periodType", PAY_DATE)
                        .queryParam("settleDecisionType", SETTLED)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse<SettlementContent>>() {}));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 정산 응답이 비어 있습니다.");
        }
        return response;
    }

    private PageResponse<DailySettlementContent> fetchDailySettlements(
            LocalDate from, LocalDate to, int page) {
        PageResponse<DailySettlementContent> response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/pay-settle/settle/daily")
                        .queryParam("startDate", from)
                        .queryParam("endDate", to)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse<DailySettlementContent>>() {}));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 일별 정산 응답이 비어 있습니다.");
        }
        return response;
    }

    private PageResponse<CommissionContent> fetchCommissions(LocalDate date, int page) {
        PageResponse<CommissionContent> response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path(
                                "/external/v1/pay-settle/settle/commission-details")
                        .queryParam("searchDate", date)
                        .queryParam("periodType", PAY_DATE)
                        .queryParam("settleDecisionType", SETTLED)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse<CommissionContent>>() {}));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 수수료 응답이 비어 있습니다.");
        }
        return response;
    }

    private PageResponse<VatContent> fetchVat(LocalDate from, LocalDate to, int page) {
        PageResponse<VatContent> response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/pay-settle/vat/daily")
                        .queryParam("startDate", from)
                        .queryParam("endDate", to)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse<VatContent>>() {}));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 부가세 응답이 비어 있습니다.");
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

    private static DailySettlement toDailySettlement(DailySettlementContent content) {
        return new DailySettlement(
                content.settleBasisStartDate(), content.settleBasisEndDate(),
                content.settleExpectDate(), content.settleCompleteDate(),
                amount(content.settleAmount()), amount(content.paySettleAmount()),
                amount(content.commissionSettleAmount()), amount(content.benefitSettleAmount()),
                amount(content.deductionRestoreSettleAmount()), amount(content.payHoldbackAmount()),
                amount(content.minusChargeAmount()), amount(content.differenceSettleAmount()),
                amount(content.returnCareSettleAmount()), amount(content.normalSettleAmount()),
                amount(content.quickSettleAmount()),
                amount(content.preferentialCommissionAmount()),
                amount(content.settlementLimitAmount()), content.settleMethodType(),
                content.merchantId(), content.merchantName());
    }

    private static CommissionDetail toCommissionDetail(CommissionContent content) {
        return new CommissionDetail(
                content.orderNo(), content.productOrderId(), content.productOrderType(),
                content.productId(), content.productName(), content.merchantId(),
                content.merchantName(), content.settleType(), content.settleBasisDate(),
                content.settleExpectDate(), content.settleCompleteDate(), content.taxReturnDate(),
                amount(content.commissionBasisAmount()), content.commissionType(),
                content.payMeansType(), amount(content.commissionAmount()),
                nullableAmount(content.maximumSellingInterlockCommissionAmount()));
    }

    private static DailyVat toDailyVat(VatContent content) {
        return new DailyVat(
                content.settleBasisDate(), amount(content.totalSalesAmount()),
                amount(content.taxationSalesAmount()), amount(content.taxExemptionSalesAmount()),
                amount(content.creditCardAmount()), amount(content.cashInComeDeductionAmount()),
                amount(content.cashOutGoingEvidenceAmount()),
                amount(content.cashExclusionIssuanceAmount()), amount(content.otherAmount()),
                content.merchantId(), content.merchantName());
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

    private record PageResponse<T>(List<T> elements, Pagination pagination, Page<T> data) {}

    private record Page<T>(List<T> elements, Pagination pagination) {}

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

    private record DailySettlementContent(
            LocalDate settleBasisStartDate,
            LocalDate settleBasisEndDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            BigDecimal settleAmount,
            BigDecimal paySettleAmount,
            BigDecimal commissionSettleAmount,
            BigDecimal benefitSettleAmount,
            BigDecimal deductionRestoreSettleAmount,
            BigDecimal payHoldbackAmount,
            BigDecimal minusChargeAmount,
            BigDecimal differenceSettleAmount,
            BigDecimal returnCareSettleAmount,
            BigDecimal normalSettleAmount,
            BigDecimal quickSettleAmount,
            BigDecimal preferentialCommissionAmount,
            BigDecimal settlementLimitAmount,
            String settleMethodType,
            String merchantId,
            String merchantName
    ) {}

    private record CommissionContent(
            String orderNo,
            String productOrderId,
            String productOrderType,
            String productId,
            String productName,
            String merchantId,
            String merchantName,
            String settleType,
            LocalDate settleBasisDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            LocalDate taxReturnDate,
            BigDecimal commissionBasisAmount,
            String commissionType,
            String payMeansType,
            BigDecimal commissionAmount,
            BigDecimal maximumSellingInterlockCommissionAmount
    ) {}

    private record VatContent(
            LocalDate settleBasisDate,
            BigDecimal totalSalesAmount,
            BigDecimal taxationSalesAmount,
            BigDecimal taxExemptionSalesAmount,
            BigDecimal creditCardAmount,
            BigDecimal cashInComeDeductionAmount,
            BigDecimal cashOutGoingEvidenceAmount,
            BigDecimal cashExclusionIssuanceAmount,
            BigDecimal otherAmount,
            String merchantId,
            String merchantName
    ) {}
}
