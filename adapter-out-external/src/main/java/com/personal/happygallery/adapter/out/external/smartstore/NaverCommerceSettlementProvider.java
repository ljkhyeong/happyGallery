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

    @Override
    public List<DailySettlement> findDailySettlements(LocalDate from, LocalDate to) {
        List<DailySettlement> results = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            DailySettlementResponse response = fetchDailySettlements(from, to, page);
            DailySettlementPage settlementPage = response.data() == null
                    ? new DailySettlementPage(response.elements(), response.pagination())
                    : response.data();
            List<DailySettlementContent> elements = settlementPage.elements() == null
                    ? List.of() : settlementPage.elements();
            elements.stream().map(NaverCommerceSettlementProvider::toDailySettlement)
                    .forEach(results::add);
            totalPages = totalPages(settlementPage.pagination(), page);
            page++;
        } while (page <= totalPages);
        return List.copyOf(results);
    }

    @Override
    public List<CommissionDetail> findCommissionDetails(LocalDate from, LocalDate to) {
        List<CommissionDetail> results = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            int page = 1;
            int totalPages;
            do {
                CommissionResponse response = fetchCommissions(date, page);
                CommissionPage commissionPage = response.data() == null
                        ? new CommissionPage(response.elements(), response.pagination())
                        : response.data();
                List<CommissionContent> elements = commissionPage.elements() == null
                        ? List.of() : commissionPage.elements();
                elements.stream().map(NaverCommerceSettlementProvider::toCommissionDetail)
                        .forEach(results::add);
                totalPages = totalPages(commissionPage.pagination(), page);
                page++;
            } while (page <= totalPages);
        }
        return List.copyOf(results);
    }

    @Override
    public List<DailyVat> findDailyVat(LocalDate from, LocalDate to) {
        List<DailyVat> results = new ArrayList<>();
        int page = 1;
        int totalPages;
        do {
            VatResponse response = fetchVat(from, to, page);
            VatPage vatPage = response.data() == null
                    ? new VatPage(response.elements(), response.pagination())
                    : response.data();
            List<VatContent> elements = vatPage.elements() == null
                    ? List.of() : vatPage.elements();
            elements.stream().map(NaverCommerceSettlementProvider::toDailyVat)
                    .forEach(results::add);
            totalPages = totalPages(vatPage.pagination(), page);
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

    private DailySettlementResponse fetchDailySettlements(
            LocalDate from, LocalDate to, int page) {
        DailySettlementResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/pay-settle/settle/daily")
                        .queryParam("startDate", from)
                        .queryParam("endDate", to)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(DailySettlementResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 일별 정산 응답이 비어 있습니다.");
        }
        return response;
    }

    private CommissionResponse fetchCommissions(LocalDate date, int page) {
        CommissionResponse response = accessTokenProvider.authorized(token -> restClient.get()
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
                .body(CommissionResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 수수료 응답이 비어 있습니다.");
        }
        return response;
    }

    private VatResponse fetchVat(LocalDate from, LocalDate to, int page) {
        VatResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri(builder -> builder.path("/external/v1/pay-settle/vat/daily")
                        .queryParam("startDate", from)
                        .queryParam("endDate", to)
                        .queryParam("pageNumber", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(VatResponse.class));
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

    private static int totalPages(Pagination pagination, int currentPage) {
        return pagination == null ? currentPage : pagination.totalPages();
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

    private record DailySettlementResponse(
            List<DailySettlementContent> elements,
            Pagination pagination,
            DailySettlementPage data
    ) {}

    private record DailySettlementPage(
            List<DailySettlementContent> elements,
            Pagination pagination
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

    private record CommissionResponse(
            List<CommissionContent> elements,
            Pagination pagination,
            CommissionPage data
    ) {}

    private record CommissionPage(
            List<CommissionContent> elements,
            Pagination pagination
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

    private record VatResponse(
            List<VatContent> elements,
            Pagination pagination,
            VatPage data
    ) {}

    private record VatPage(List<VatContent> elements, Pagination pagination) {}

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
