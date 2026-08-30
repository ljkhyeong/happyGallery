package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class DeliveryApiRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    DeliveryApiRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean
    CloseableHttpClient deliveryApiHttpClient(DeliveryApiProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    RestClient deliveryApiRestClient(
            RestClient.Builder builder,
            DeliveryApiProperties properties,
            @Qualifier("deliveryApiHttpClient") CloseableHttpClient httpClient) {
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.apiKey() + ":" + properties.secretKey())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
