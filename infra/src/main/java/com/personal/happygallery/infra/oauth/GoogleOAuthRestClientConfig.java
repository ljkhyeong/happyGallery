package com.personal.happygallery.infra.oauth;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("prod")
class GoogleOAuthRestClientConfig {

    @Bean
    RestClient googleOAuthRestClient(GoogleOAuthProperties props) {
        return RestClient.builder()
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
