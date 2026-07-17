package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("prod")
class NaverOAuthRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    NaverOAuthRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient naverOAuthHttpClient(NaverOAuthProperties props) {
        return pooledHttpClientFactory.create(props);
    }

    @Bean
    RestClient naverOAuthRestClient(NaverOAuthProperties props,
                                    @Qualifier("naverOAuthHttpClient") CloseableHttpClient httpClient) {
        return RestClient.builder()
                .requestFactory(pooledHttpClientFactory.requestFactory(httpClient))
                .build();
    }
}
