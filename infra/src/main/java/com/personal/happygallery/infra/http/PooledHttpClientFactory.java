package com.personal.happygallery.infra.http;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public final class PooledHttpClientFactory {

    private PooledHttpClientFactory() {
    }

    public static CloseableHttpClient create(long connectTimeoutMillis,
                                             long readTimeoutMillis,
                                             long acquireTimeoutMillis,
                                             int maxConnections,
                                             long keepAliveMillis) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMillis))
                .setSocketTimeout(Timeout.ofMilliseconds(readTimeoutMillis))
                .setTimeToLive(TimeValue.ofMilliseconds(keepAliveMillis))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(maxConnections)
                .setMaxConnPerRoute(maxConnections)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(acquireTimeoutMillis))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMillis))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy((response, context) -> {
                    TimeValue keepAlive = DefaultConnectionKeepAliveStrategy.INSTANCE
                            .getKeepAliveDuration(response, context);
                    if (keepAlive != null && keepAlive.toMilliseconds() > 0) {
                        return keepAlive;
                    }
                    return TimeValue.ofMilliseconds(keepAliveMillis);
                })
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(keepAliveMillis))
                .build();
    }

    public static HttpComponentsClientHttpRequestFactory requestFactory(CloseableHttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
