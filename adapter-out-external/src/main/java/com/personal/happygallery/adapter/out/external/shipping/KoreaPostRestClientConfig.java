package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class KoreaPostRestClientConfig {
    @Bean
    CloseableHttpClient koreaPostHttpClient(KoreaPostProperties properties, PooledHttpClientFactory factory) {
        return factory.create(properties, builder -> builder.disableRedirectHandling().disableAutomaticRetries());
    }

    @Bean
    RestClient koreaPostRestClient(RestClient.Builder builder,
            @Qualifier("koreaPostHttpClient") CloseableHttpClient httpClient) {
        // 우정사업본부 공식 명세의 HTTP 주소다. 서비스키가 다른 호스트로 전달되지 않게 리다이렉트를 끈다.
        return builder.baseUrl("http://openapi.epost.go.kr")
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
