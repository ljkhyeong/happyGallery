package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.StockCommand;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider.ProductCommand;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmartStoreRestClientConfigTest {

    private static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30));
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);
    private MockRestServiceServer server;
    private NaverCommerceInventoryProvider provider;

    @BeforeEach
    void setUp() {
        var config = new SmartStoreRestClientConfig(new PooledHttpClientFactory());
        RestClient.Builder builder = config.smartStoreRestClient(
                RestClient.builder(), PROPERTIES, mock(CloseableHttpClient.class)).mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        var client = builder.build();
        provider = new NaverCommerceInventoryProvider(client, PROPERTIES,
                new NaverCommerceAccessTokenProvider(client, PROPERTIES, CLOCK));
        server.expect(requestTo(PROPERTIES.baseUrl() + "/external/v1/oauth2/token"))
                .andRespond(withSuccess("""
                        {"access_token":"access-token","expires_in":10800,"token_type":"Bearer"}
                        """, MediaType.APPLICATION_JSON));
    }

    @ParameterizedTest
    @CsvSource({"200,true", "204,true", "301,false", "302,false", "307,false", "308,false", "400,false", "503,false"})
    @DisplayName("재고 반영은 2xx 응답만 성공으로 처리하고 주소 변경과 HTTP 오류를 실패로 분류한다")
    void sync_classifiesResponseStatus(int status, boolean success) {
        server.expect(requestTo(PROPERTIES.baseUrl() + "/external/v1/products/origin-products/multi-update"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatusCode.valueOf(status))
                        .location(URI.create("https://redirect.example/stock")));

        var result = provider.sync(new StockCommand(123456789L, 7, List.of()));

        server.verify();
        assertThat(result.success()).isEqualTo(success);
    }

    @ParameterizedTest
    @ValueSource(ints = {307, 308})
    @DisplayName("가격·재고 반영이 주소 변경 응답이면 뒤이은 판매 상태 변경을 실행하지 않는다")
    void applyProduct_redirect_stopsBeforeStatusChange(int status) {
        server.expect(requestTo(PROPERTIES.baseUrl() + "/external/v1/products/origin-products/multi-update"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatusCode.valueOf(status))
                        .location(URI.create("https://redirect.example/product")));

        var result = provider.applyProduct(new ProductCommand(123456789L, 35000L, "SALE", 7, List.of()));

        server.verify();
        assertThat(result.success()).isFalse();
    }
}
