package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("prod")
class TossPaymentsRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    TossPaymentsRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient tossPaymentsHttpClient(TossPaymentsProperties props) {
        return pooledHttpClientFactory.create(props);
    }

    @Bean
    RestClient tossPaymentsRestClient(RestClient.Builder builder,
                                      TossPaymentsProperties props,
                                      @Qualifier("tossPaymentsHttpClient") CloseableHttpClient httpClient) {
        return configure(builder, props)
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    static RestClient.Builder configure(RestClient.Builder builder, TossPaymentsProperties props) {
        Assert.hasText(props.secretKey(), "prod 프로필에는 TOSS_SECRET_KEY가 필요합니다.");
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(props.secretKey(), ""));
    }
}
