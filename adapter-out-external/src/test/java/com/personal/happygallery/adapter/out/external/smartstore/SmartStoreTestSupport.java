package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.adapter.out.external.http.HttpPool;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 스마트스토어 provider 테스트가 공유하는 연동 설정·고정 시각·토큰 발급 stub·mock 서버 조립. */
final class SmartStoreTestSupport {

    static final SmartStoreProperties PROPERTIES = new SmartStoreProperties(
            true, "client-id", "$2a$10$abcdefghijklmnopqrstuv", "SELF", "",
            "https://api.commerce.naver.com", new HttpPool(Duration.ofSeconds(5), Duration.ofSeconds(1),
            Duration.ofMillis(500), 5, Duration.ofSeconds(30)));
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-29T03:00:00Z"), ZoneOffset.UTC);

    private static final String TOKEN_URL = PROPERTIES.baseUrl() + "/external/v1/oauth2/token";

    private SmartStoreTestSupport() {
    }

    /** provider와 토큰 provider가 같은 mock 서버를 호출하도록 RestClient 하나로 묶어 만든다. */
    static MockedApi mockApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient client = builder.build();
        return new MockedApi(server, client, new NaverCommerceAccessTokenProvider(client, PROPERTIES, CLOCK));
    }

    /**
     * 첫 토큰 발급이 "access-token"으로 성공하는 stub.
     * 토큰 요청 본문 계약은 {@code NaverCommerceInventoryProviderTest}가 한 곳에서 검증한다.
     */
    static void expectToken(MockRestServiceServer server) {
        expectToken(server, "access-token");
    }

    static void expectToken(MockRestServiceServer server, String accessToken) {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("""
                        {"access_token":"%s","expires_in":10800,"token_type":"Bearer"}
                        """.formatted(accessToken), MediaType.APPLICATION_JSON));
    }

    /** provider 생성자에 넘길 RestClient·토큰 provider와 둘의 호출을 모두 받는 mock 서버. */
    record MockedApi(MockRestServiceServer server, RestClient client,
                     NaverCommerceAccessTokenProvider tokenProvider) {
    }
}
