package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.OptionStock;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.StockCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NaverCommerceInventoryProviderTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true,
            "client-id",
            "$2a$10$abcdefghijklmnopqrstuv",
            "SELF",
            "",
            "https://api.commerce.naver.com",
            Duration.ofSeconds(5),
            Duration.ofSeconds(1),
            Duration.ofMillis(500),
            5,
            Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("기성품 재고는 인증 토큰을 발급받아 원상품 재고 절대값으로 전송한다")
    void sync_readyStock_sendsAbsoluteStockQuantity() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider accessTokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceInventoryProvider provider = new NaverCommerceInventoryProvider(
                builder.build(), PROPERTIES, accessTokenProvider);

        expectToken(server);
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/products/origin-products/multi-update"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(content().json("""
                        {
                          "multiProductUpdateRequestVos": [{
                            "originProductNo": 123456789,
                            "multiUpdateTypes": ["STOCK"],
                            "stockQuantity": 7
                          }]
                        }
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var result = provider.sync(new StockCommand(123456789L, 7, List.of()));

        server.verify();
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("주문제작품 재고는 한 원상품의 모든 옵션 수량을 한 요청으로 전송한다")
    void sync_optionProduct_sendsAllOptionStocksTogether() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider accessTokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceInventoryProvider provider = new NaverCommerceInventoryProvider(
                builder.build(), PROPERTIES, accessTokenProvider);

        expectToken(server);
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/products/origin-products/123456789/option-stock"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(content().json("""
                        {
                          "optionInfo": {
                            "optionCombinations": [
                              {"id": 11, "stockQuantity": 3},
                              {"id": 12, "stockQuantity": 0}
                            ],
                            "useStockManagement": true
                          }
                        }
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var result = provider.sync(new StockCommand(
                123456789L,
                null,
                List.of(new OptionStock(11L, 3), new OptionStock(12L, 0))));

        server.verify();
        assertThat(result.success()).isTrue();
    }

    @Test
    @DisplayName("원상품 차이를 조회하고 판매가·옵션가·판매 상태를 명시적으로 반영한다")
    void getAndApplyProduct_usesProductContracts() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NaverCommerceAccessTokenProvider accessTokenProvider = new NaverCommerceAccessTokenProvider(
                builder.build(), PROPERTIES, CLOCK);
        NaverCommerceInventoryProvider provider = new NaverCommerceInventoryProvider(
                builder.build(), PROPERTIES, accessTokenProvider);

        expectToken(server);
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v2/products/origin-products/123456789"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "originProduct":{
                            "salePrice":33000,
                            "statusType":"SALE",
                            "detailAttribute":{"optionInfo":{"optionCombinations":[
                              {"id":11,"stockQuantity":3,"price":1000,"usable":true}
                            ]}}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/products/origin-products/123456789/option-stock"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {
                          "productSalePrice":{"salePrice":35000},
                          "optionInfo":{
                            "optionCombinations":[
                              {"id":11,"stockQuantity":3,"price":2000,"usable":true}
                            ],
                            "useStockManagement":true
                          }
                        }
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(
                        "https://api.commerce.naver.com/external/v1/products/origin-products/123456789/change-status"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"statusType\":\"SALE\"}"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var product = provider.getProduct(123456789L);
        var result = provider.applyProduct(new ProductCommand(
                123456789L, 35000L, "SALE", null,
                List.of(new ProductOption(11L, 3, 2000L, true))));

        server.verify();
        assertThat(product.salePrice()).isEqualTo(33000L);
        assertThat(product.options()).singleElement()
                .satisfies(option -> assertThat(option.price()).isEqualTo(1000L));
        assertThat(result.success()).isTrue();
    }

    private static void expectToken(MockRestServiceServer server) {
        server.expect(requestTo("https://api.commerce.naver.com/external/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("client_id=client-id")))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andExpect(content().string(containsString("type=SELF")))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "expires_in": 10800,
                          "token_type": "Bearer"
                        }
                        """, MediaType.APPLICATION_JSON));
    }
}
