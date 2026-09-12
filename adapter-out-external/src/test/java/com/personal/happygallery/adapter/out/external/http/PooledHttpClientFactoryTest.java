package com.personal.happygallery.adapter.out.external.http;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
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

    @ParameterizedTest
    @CsvSource({"GET,302", "POST,307", "POST,308"})
    @DisplayName("리다이렉트 주소에 인증 헤더와 본문을 보내지 않고 원래 응답을 전달한다")
    void create_doesNotFollowRedirects(HttpMethod method, int status) throws Exception {
        HttpPoolProperties properties = mock(HttpPoolProperties.class);
        when(properties.connectTimeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.timeout()).thenReturn(Duration.ofSeconds(3));
        when(properties.acquireTimeout()).thenReturn(Duration.ofSeconds(1));
        when(properties.maxConnections()).thenReturn(1);
        when(properties.keepAlive()).thenReturn(Duration.ofSeconds(30));
        AtomicInteger redirectedRequests = new AtomicInteger();
        AtomicReference<String> forwardedSecret = new AtomicReference<>();
        AtomicReference<String> forwardedBody = new AtomicReference<>();
        HttpServer target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        target.createContext("/", exchange -> {
            redirectedRequests.incrementAndGet();
            forwardedSecret.set(exchange.getRequestHeaders().getFirst("X-Secret-Key"));
            forwardedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin.createContext("/", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + target.getAddress().getPort());
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        target.start();
        origin.start();

        try (var client = new PooledHttpClientFactory().create(properties)) {
            RestClient rest = RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + origin.getAddress().getPort())
                    .requestFactory(new HttpComponentsClientHttpRequestFactory(client)).build();
            var request = rest.method(method).uri("/").header("X-Secret-Key", "test-api-secret");
            if (method == HttpMethod.POST) {
                request.body("verificationCode=test-only");
            }

            int actualStatus = request.exchange((sent, response) -> response.getStatusCode().value());

            assertSoftly(softly -> {
                softly.assertThat(actualStatus).isEqualTo(status);
                softly.assertThat(redirectedRequests).hasValue(0);
                softly.assertThat(forwardedSecret).hasNullValue();
                softly.assertThat(forwardedBody).hasNullValue();
            });
        } finally {
            origin.stop(0);
            target.stop(0);
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
