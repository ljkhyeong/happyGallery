package com.personal.happygallery.adapter.out.external.holiday;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class PublicHolidayRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    PublicHolidayRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean
    CloseableHttpClient publicHolidayHttpClient(PublicHolidayApiProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    RestClient publicHolidayRestClient(
            RestClient.Builder builder,
            PublicHolidayApiProperties properties,
            @Qualifier("publicHolidayHttpClient") CloseableHttpClient httpClient) {
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
