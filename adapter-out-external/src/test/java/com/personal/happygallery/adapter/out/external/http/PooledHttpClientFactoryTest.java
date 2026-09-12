package com.personal.happygallery.adapter.out.external.http;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PooledHttpClientFactoryTest {

    @ParameterizedTest
    @CsvSource({"GET,429", "GET,503", "POST,429", "POST,503"})
    @DisplayName("호출 제한과 장애 응답을 자동 재전송 없이 호출부에 전달한다")
    void create_doesNotRetryErrorResponses(HttpMethod method, int status) throws Exception {
        HttpPoolProperties properties = mock(HttpPoolProperties.class);
        when(properties.connectTimeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.timeout()).thenReturn(Duration.ofSeconds(3));
        when(properties.acquireTimeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.maxConnections()).thenReturn(1);
        when(properties.keepAlive()).thenReturn(Duration.ofSeconds(30));
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Retry-After", "1");
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();

        try (var client = new PooledHttpClientFactory().create(properties)) {
            RestClient rest = RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(client)).build();
            var request = rest.method(method).uri("/");
            if (method == HttpMethod.POST) {
                request.body("test");
            }

            int actualStatus = request.exchange((sent, response) -> response.getStatusCode().value());

            assertThat(actualStatus).isEqualTo(status);
            assertThat(requests).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("생성한 HTTP 클라이언트는 요청에 설정된 연결 유지 시간을 적용한다")
    void create_appliesConfiguredKeepAlive() throws Exception {
        HttpPoolProperties properties = mock(HttpPoolProperties.class);
        when(properties.connectTimeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.timeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.acquireTimeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.maxConnections()).thenReturn(1);
        when(properties.keepAlive()).thenReturn(Duration.ofSeconds(37));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try (var client = new PooledHttpClientFactory().create(properties)) {
            HttpClientContext context = HttpClientContext.create();
            var request = new HttpGet("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            try (var response = client.executeOpen(null, request, context)) {
                assertThat(response.getCode()).isEqualTo(200);
                assertThat(context.getRequestConfigOrDefault().getConnectionKeepAlive())
                        .isEqualTo(TimeValue.ofSeconds(37));
            }
        } finally {
            server.stop(0);
        }
    }
}
