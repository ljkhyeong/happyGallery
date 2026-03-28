package com.personal.happygallery.infra.notification;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * prod 프로필 전용 RestClient 빈 설정.
 * 카카오 알림톡과 NHN SMS 발송에 사용한다.
 */
@Configuration
@Profile("prod")
class NotificationRestClientConfig {

    @Bean
    RestClient kakaoRestClient(KakaoNotificationProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "KakaoAK " + props.apiKey())
                .requestFactory(clientHttpRequestFactory(props.timeoutMillis()))
                .build();
    }

    @Bean
    RestClient smsRestClient(SmsNotificationProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Secret-Key", props.apiSecret())
                .requestFactory(clientHttpRequestFactory(props.timeoutMillis()))
                .build();
    }

    private static SimpleClientHttpRequestFactory clientHttpRequestFactory(long timeoutMillis) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMillis));
        factory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        return factory;
    }
}
