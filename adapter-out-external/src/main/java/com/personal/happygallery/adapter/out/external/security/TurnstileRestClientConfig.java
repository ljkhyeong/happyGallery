package com.personal.happygallery.adapter.out.external.security;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Configuration(proxyBeanMethods = false)
class TurnstileRestClientConfig {
    @Bean
    CloseableHttpClient turnstileHttpClient(TurnstileProperties properties, PooledHttpClientFactory factory) {
        return factory.create(properties);
    }

    @Bean
    RestClient turnstileRestClient(RestClient.Builder builder,
            @Qualifier("turnstileHttpClient") CloseableHttpClient httpClient) {
        return builder.baseUrl("https://challenges.cloudflare.com")
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .defaultStatusHandler(status -> !status.is2xxSuccessful(), (request, response) -> {
                    throw new RestClientException("Turnstile 검증 응답 오류");
                })
                .build();
    }
}
