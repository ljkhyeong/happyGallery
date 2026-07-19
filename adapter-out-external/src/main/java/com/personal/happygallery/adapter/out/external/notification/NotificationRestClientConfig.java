package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * prod 프로필 전용 RestClient 빈 설정.
 * NHN Cloud Alimtalk과 SMS 발송에 사용한다.
 */
@Configuration
@Profile("prod")
class NotificationRestClientConfig {

    private final PooledHttpClientFactory pooledHttpClientFactory;

    NotificationRestClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient alimtalkHttpClient(AlimtalkNotificationProperties props) {
        return pooledHttpClientFactory.create(props);
    }

    @Bean
    RestClient alimtalkRestClient(AlimtalkNotificationProperties props,
                                  @Qualifier("alimtalkHttpClient") CloseableHttpClient httpClient) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .requestFactory(pooledHttpClientFactory.requestFactory(httpClient))
                .build();
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient smsHttpClient(SmsNotificationProperties props) {
        return pooledHttpClientFactory.create(props);
    }

    @Bean
    RestClient smsRestClient(SmsNotificationProperties props,
                             @Qualifier("smsHttpClient") CloseableHttpClient httpClient) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .defaultHeader("X-Secret-Key", props.apiSecret())
                .requestFactory(pooledHttpClientFactory.requestFactory(httpClient))
                .build();
    }
}
