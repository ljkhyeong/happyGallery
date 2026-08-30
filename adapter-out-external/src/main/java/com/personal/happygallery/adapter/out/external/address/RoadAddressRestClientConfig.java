package com.personal.happygallery.adapter.out.external.address;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class RoadAddressRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    RoadAddressRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean
    CloseableHttpClient roadAddressHttpClient(RoadAddressProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    RestClient roadAddressRestClient(
            RestClient.Builder builder,
            RoadAddressProperties properties,
            @Qualifier("roadAddressHttpClient") CloseableHttpClient httpClient) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
