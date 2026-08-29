package com.personal.happygallery.adapter.out.external.smartstore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceSettlementProviderTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("지급일 기준 정산 내역과 수수료를 공식 페이지 응답에서 읽는다")
    void findByPayDate_readsSettlementPage() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceSettlementProvider provider = new NaverCommerceSettlementProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));

        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {"access_token":"access-token","expires_in":10800,"token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString(
                        "/external/v1/pay-settle/settle/case")))
                .andExpect(queryParam("searchDate", "2026-08-29"))
                .andExpect(queryParam("periodType", "SETTLE_CASEBYCASE_PAY_DATE"))
                .andExpect(queryParam("settleDecisionType", "SETTLED"))
                .andExpect(queryParam("pageNumber", "1"))
                .andExpect(queryParam("pageSize", "1000"))
                .andRespond(withSuccess("""
                        {
                          "elements":[{
                            "settleBasisDate":"2026-08-27",
                            "settleExpectDate":"2026-08-29",
                            "settleCompleteDate":"2026-08-29",
                            "payDate":"2026-08-29",
                            "orderId":"order-1",
                            "productOrderId":"po-1",
                            "productOrderType":"PROD_ORDER",
                            "settleType":"NORMAL_SETTLE_ORIGINAL",
                            "productName":"가죽 지갑",
                            "paySettleAmount":70000,
                            "totalPayCommissionAmount":1000,
                            "sellingInterlockCommissionAmount":2000,
                            "benefitSettleAmount":0,
                            "settleExpectAmount":67000
                          }],
                          "pagination":{"page":1,"size":1000,"totalPages":1,"totalElements":1}
                        }
                        """, MediaType.APPLICATION_JSON));

        var items = provider.findByPayDate(LocalDate.of(2026, 8, 29));

        server.verify();
        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.productOrderId()).isEqualTo("po-1");
            assertThat(item.paySettleAmount()).isEqualTo(70000L);
            assertThat(item.settleExpectAmount()).isEqualTo(67000L);
        });
    }

    @Test
    @DisplayName("일별 정산과 수수료와 부가세 회계 자료를 각각 조회한다")
    void findAccountingData_readsOfficialReportContracts() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceSettlementProvider provider = new NaverCommerceSettlementProvider(
                builder.build(), PROPERTIES,
                new NaverCommerceAccessTokenProvider(builder.build(), PROPERTIES, CLOCK));

        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {"access_token":"access-token","expires_in":10800,"token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/external/v1/pay-settle/settle/daily")))
                .andExpect(queryParam("startDate", "2026-07-01"))
                .andExpect(queryParam("endDate", "2026-07-01"))
                .andRespond(withSuccess("""
                        {"elements":[{
                          "settleBasisStartDate":"2026-07-01","settleBasisEndDate":"2026-07-01",
                          "settleExpectDate":"2026-07-03","settleCompleteDate":"2026-07-03",
                          "settleAmount":9000,"paySettleAmount":10000,"commissionSettleAmount":1000,
                          "benefitSettleAmount":0,"deductionRestoreSettleAmount":0,
                          "payHoldbackAmount":0,"minusChargeAmount":0,"differenceSettleAmount":0,
                          "returnCareSettleAmount":0,"normalSettleAmount":9000,
                          "quickSettleAmount":0,"preferentialCommissionAmount":0,
                          "settlementLimitAmount":0,"settleMethodType":"BANK",
                          "merchantId":"merchant-1","merchantName":"해피갤러리"
                        }],"pagination":{"page":1,"size":1000,"totalPages":1,"totalElements":1}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString(
                        "/external/v1/pay-settle/settle/commission-details")))
                .andExpect(queryParam("searchDate", "2026-07-01"))
                .andRespond(withSuccess("""
                        {"elements":[{
                          "orderNo":"order-1","productOrderId":"po-1",
                          "productOrderType":"PROD_ORDER","productId":"product-1",
                          "productName":"가죽 지갑","merchantId":"merchant-1",
                          "merchantName":"해피갤러리","settleType":"NORMAL_SETTLE_ORIGINAL",
                          "settleBasisDate":"2026-07-01","settleExpectDate":"2026-07-03",
                          "settleCompleteDate":"2026-07-03","taxReturnDate":"2026-07-31",
                          "commissionBasisAmount":10000,"commissionType":"SALES_COMMISSION",
                          "payMeansType":"CARD","commissionAmount":1000,
                          "maximumSellingInterlockCommissionAmount":500
                        }],"pagination":{"page":1,"size":1000,"totalPages":1,"totalElements":1}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/external/v1/pay-settle/vat/daily")))
                .andExpect(queryParam("startDate", "2026-07-01"))
                .andExpect(queryParam("endDate", "2026-07-01"))
                .andRespond(withSuccess("""
                        {"elements":[{
                          "settleBasisDate":"2026-07-01","totalSalesAmount":10000,
                          "taxationSalesAmount":10000,"taxExemptionSalesAmount":0,
                          "creditCardAmount":10000,"cashInComeDeductionAmount":0,
                          "cashOutGoingEvidenceAmount":0,"cashExclusionIssuanceAmount":0,
                          "otherAmount":0,"merchantId":"merchant-1","merchantName":"해피갤러리"
                        }],"pagination":{"page":1,"size":1000,"totalPages":1,"totalElements":1}}
                        """, MediaType.APPLICATION_JSON));

        LocalDate date = LocalDate.of(2026, 7, 1);
        var settlements = provider.findDailySettlements(date, date);
        var commissions = provider.findCommissionDetails(date, date);
        var vat = provider.findDailyVat(date, date);

        server.verify();
        assertThat(settlements).singleElement()
                .satisfies(item -> assertThat(item.settleAmount()).isEqualTo(9000L));
        assertThat(commissions).singleElement()
                .satisfies(item -> assertThat(item.commissionAmount()).isEqualTo(1000L));
        assertThat(vat).singleElement()
                .satisfies(item -> assertThat(item.totalSalesAmount()).isEqualTo(10000L));
    }
}
