package com.personal.happygallery.adapter.out.external.http;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PooledHttpClientFactoryTest {

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
