package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

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
                .defaultStatusHandler(HttpStatusCode::is3xxRedirection, (request, response) -> {
                    throw new RestClientResponseException(
                            "스마트스토어 API가 주소 변경을 요청했습니다. 연동 주소를 확인해 주세요.",
                            response.getStatusCode().value(), "", null, null, null);
                })
                .build();
    }
}
