package com.personal.happygallery.adapter.out.external.http;

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

@Component
public class PooledHttpClientFactory {

    public CloseableHttpClient create(HttpPoolProperties props) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(props.connectTimeout()))
                .setSocketTimeout(Timeout.of(props.timeout()))
                .setTimeToLive(TimeValue.of(props.keepAlive()))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(props.maxConnections())
                .setMaxConnPerRoute(props.maxConnections())
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(props.acquireTimeout()))
                .setResponseTimeout(Timeout.of(props.timeout()))
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setKeepAliveStrategy((response, context) -> {
                    TimeValue keepAlive = DefaultConnectionKeepAliveStrategy.INSTANCE
                            .getKeepAliveDuration(response, context);
                    if (TimeValue.isPositive(keepAlive)) {
                        return keepAlive;
                    }
                    return TimeValue.of(props.keepAlive());
                })
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(props.keepAlive()))
                .build();
    }
}
