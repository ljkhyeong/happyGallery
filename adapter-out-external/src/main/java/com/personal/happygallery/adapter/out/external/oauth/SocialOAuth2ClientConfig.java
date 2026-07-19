package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
class SocialOAuth2ClientConfig {

    private static final String GOOGLE = "google";
    private static final String NAVER = "naver";

    private final PooledHttpClientFactory pooledHttpClientFactory;

    SocialOAuth2ClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient googleOAuthHttpClient(GoogleOAuthProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean(destroyMethod = "close")
    CloseableHttpClient naverOAuthHttpClient(NaverOAuthProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> socialOAuth2AccessTokenResponseClient(
            @Qualifier("googleOAuthHttpClient") CloseableHttpClient googleHttpClient,
            @Qualifier("naverOAuthHttpClient") CloseableHttpClient naverHttpClient) {
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> google =
                tokenResponseClient(googleHttpClient, false);
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> naver =
                tokenResponseClient(naverHttpClient, true);

        return request -> switch (request.getClientRegistration().getRegistrationId()) {
            case GOOGLE -> google.getTokenResponse(request);
            case NAVER -> naver.getTokenResponse(request);
            default -> throw new OAuth2AuthorizationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        };
    }

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> naverOAuth2UserService(
            @Qualifier("naverOAuthHttpClient") CloseableHttpClient httpClient) {
        DefaultOAuth2UserService userService = new DefaultOAuth2UserService();
        userService.setRestOperations(userInfoRestOperations(httpClient));
        userService.setAttributesConverter(request -> attributes -> flattenNaverResponse(attributes));
        return userService;
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> googleOidcUserService(
            @Qualifier("googleOAuthHttpClient") CloseableHttpClient httpClient) {
        DefaultOAuth2UserService userInfoService = new DefaultOAuth2UserService();
        userInfoService.setRestOperations(userInfoRestOperations(httpClient));

        OidcUserService oidcUserService = new OidcUserService();
        oidcUserService.setOauth2UserService(userInfoService);
        return oidcUserService;
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient(
            CloseableHttpClient httpClient, boolean includeState) {
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(tokenRestClient(httpClient));
        if (includeState) {
            client.addParametersConverter(request -> {
                LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
                parameters.set(OAuth2ParameterNames.STATE,
                        request.getAuthorizationExchange().getAuthorizationRequest().getState());
                return parameters;
            });
        }
        return client;
    }

    private RestClient tokenRestClient(CloseableHttpClient httpClient) {
        return RestClient.builder()
                .requestFactory(pooledHttpClientFactory.requestFactory(httpClient))
                .configureMessageConverters(builder -> builder
                        .configureMessageConvertersList(converters -> {
                            converters.clear();
                            converters.add(new FormHttpMessageConverter());
                            converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                        }))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
    }

    private RestOperations userInfoRestOperations(CloseableHttpClient httpClient) {
        RestTemplate restTemplate = new RestTemplate(pooledHttpClientFactory.requestFactory(httpClient));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        return restTemplate;
    }

    private Map<String, Object> flattenNaverResponse(Map<String, Object> attributes) {
        Object response = attributes.get("response");
        if (!"00".equals(attributes.get("resultcode"))
                || !"success".equals(attributes.get("message"))
                || !(response instanceof Map<?, ?> profile)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response"));
        }

        Map<String, Object> flattened = new LinkedHashMap<>(attributes);
        profile.forEach((key, value) -> {
            if (key instanceof String attributeName) {
                flattened.put(attributeName, value);
            }
        });
        return flattened;
    }
}
