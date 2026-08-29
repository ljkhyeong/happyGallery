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
}
