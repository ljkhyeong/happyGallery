package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
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
        Assert.hasText(props.appKey(), "prod 프로필에는 ALIMTALK_APP_KEY가 필요합니다.");
        Assert.hasText(props.secretKey(), "prod 프로필에는 ALIMTALK_SECRET_KEY가 필요합니다.");
        Assert.hasText(props.senderKey(), "prod 프로필에는 ALIMTALK_SENDER_KEY가 필요합니다.");
        return pooledHttpClientFactory.create(props);
    }

    @Bean
    RestClient alimtalkRestClient(RestClient.Builder builder,
                                  AlimtalkNotificationProperties props,
                                  @Qualifier("alimtalkHttpClient") CloseableHttpClient httpClient) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient smsHttpClient(SmsNotificationProperties props) {
        Assert.hasText(props.apiKey(), "prod 프로필에는 SMS_API_KEY가 필요합니다.");
        Assert.hasText(props.apiSecret(), "prod 프로필에는 SMS_API_SECRET가 필요합니다.");
        Assert.hasText(props.senderNumber(), "prod 프로필에는 SMS_SENDER_NUMBER가 필요합니다.");
        return pooledHttpClientFactory.create(props);
    }

    @Bean
    RestClient smsRestClient(RestClient.Builder builder,
                             SmsNotificationProperties props,
                             @Qualifier("smsHttpClient") CloseableHttpClient httpClient) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .defaultHeader("X-Secret-Key", props.apiSecret())
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
