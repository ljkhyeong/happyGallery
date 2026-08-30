package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class SmartStoreRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    SmartStoreRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean
    CloseableHttpClient smartStoreHttpClient(SmartStoreProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    RestClient smartStoreRestClient(
            RestClient.Builder builder,
            SmartStoreProperties properties,
            @Qualifier("smartStoreHttpClient") CloseableHttpClient httpClient) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
