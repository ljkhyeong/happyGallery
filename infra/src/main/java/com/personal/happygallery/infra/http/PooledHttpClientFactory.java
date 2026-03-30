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
import org.springframework.stereotype.Component;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Component
public class PooledHttpClientFactory {

    public CloseableHttpClient create(HttpPoolProperties props) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(props.connectTimeoutMillis()))
                .setSocketTimeout(Timeout.ofMilliseconds(props.timeoutMillis()))
                .setTimeToLive(TimeValue.ofMilliseconds(props.keepAliveMillis()))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(props.maxConnections())
                .setMaxConnPerRoute(props.maxConnections())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(props.acquireTimeoutMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(props.timeoutMillis()))
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
                    return TimeValue.ofMilliseconds(props.keepAliveMillis());
                })
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMilliseconds(props.keepAliveMillis()))
                .build();
    }

    public HttpComponentsClientHttpRequestFactory requestFactory(CloseableHttpClient httpClient) {
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
