package com.personal.happygallery.infra.notification;

import com.personal.happygallery.infra.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

/**
 * prod 프로필 전용 RestClient 빈 설정.
 * 카카오 알림톡과 NHN SMS 발송에 사용한다.
 */
@Configuration
@Profile("prod")
class NotificationRestClientConfig {

    @Bean(destroyMethod = "close")
    CloseableHttpClient kakaoHttpClient(KakaoNotificationProperties props) {
        return PooledHttpClientFactory.create(
                props.connectTimeoutMillis(),
                props.timeoutMillis(),
                props.acquireTimeoutMillis(),
                props.maxConnections(),
                props.keepAliveMillis()
        );
    }

    @Bean
    RestClient kakaoRestClient(KakaoNotificationProperties props,
                               @Qualifier("kakaoHttpClient") CloseableHttpClient httpClient) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "KakaoAK " + props.apiKey())
                .requestFactory(PooledHttpClientFactory.requestFactory(httpClient))
                .build();
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient smsHttpClient(SmsNotificationProperties props) {
        return PooledHttpClientFactory.create(
                props.connectTimeoutMillis(),
                props.timeoutMillis(),
                props.acquireTimeoutMillis(),
                props.maxConnections(),
                props.keepAliveMillis()
        );
    }

    @Bean
    RestClient smsRestClient(SmsNotificationProperties props,
                             @Qualifier("smsHttpClient") CloseableHttpClient httpClient) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Secret-Key", props.apiSecret())
                .requestFactory(PooledHttpClientFactory.requestFactory(httpClient))
                .build();
    }
}
