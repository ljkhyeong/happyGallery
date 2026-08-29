package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.ChangeCursor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceOrderProviderTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("변경 주문 식별자를 조회한 뒤 상품 주문 상세와 옵션 아이템 번호를 읽는다")
    void fetchChangedOrders_readsDetailAndItemNumber() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider tokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceOrderProvider provider = new NaverCommerceOrderProvider(
                builder.build(), PROPERTIES, tokenProvider);

        expectToken(server);
        server.expect(requestTo(containsString(
                        "/external/v1/pay-order/seller/product-orders/last-changed-statuses")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(queryParam("lastChangedFrom", "2026-08-29T11:50:00+09:00"))
                .andExpect(queryParam("lastChangedTo", "2026-08-29T12:00:00+09:00"))
                .andExpect(queryParam("limitCount", "300"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "count": 1,
                            "lastChangeStatuses": [{
                              "productOrderId": "2026082912345678",
                              "lastChangedType": "PAYED",
                              "lastChangedDate": "2026-08-29T11:59:00+09:00"
                            }]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/pay-order/seller/product-orders/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "productOrderIds": ["2026082912345678"],
                          "quantityClaimCompatibility": true
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "data": [{
                            "order": {
                              "orderId": "2026082911111111",
                              "paymentDate": "2026-08-29T11:58:00+09:00"
                            },
                            "productOrder": {
                              "productOrderId": "2026082912345678",
                              "originalProductId": "123456789",
                              "itemNo": "90001",
                              "productName": "각인 카드지갑",
                              "productOption": "색상: 브라운",
                              "productOrderStatus": "PAYED",
                              "initialQuantity": 2,
                              "remainQuantity": 2
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        var page = provider.fetchChanges(
                new ChangeCursor(LocalDateTime.of(2026, 8, 29, 11, 50), null),
                LocalDateTime.of(2026, 8, 29, 12, 0));
        var details = provider.fetchDetails(List.of("2026082912345678"));

        server.verify();
        assertThat(page.changes()).hasSize(1);
        assertThat(details.getFirst().originProductNo()).isEqualTo(123456789L);
        assertThat(details.getFirst().itemNo()).isEqualTo(90001L);
        assertThat(details.getFirst().remainQuantity()).isEqualTo(2);
    }

    private static void expectToken(MockRestServiceServer server) {
        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "expires_in": 10800,
                          "token_type": "Bearer"
                        }
                        """, MediaType.APPLICATION_JSON));
    }
}
