package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.adapter.out.external.http.PooledHttpClientFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

@Configuration(proxyBeanMethods = false)
class SocialOAuth2ClientConfig {

    private static final String GOOGLE = "google";
    private static final String NAVER = "naver";
    private static final String KAKAO = "kakao";

    private final PooledHttpClientFactory pooledHttpClientFactory;

    SocialOAuth2ClientConfig(PooledHttpClientFactory pooledHttpClientFactory) {
        this.pooledHttpClientFactory = pooledHttpClientFactory;
    }

    @Bean
    CloseableHttpClient googleOAuthHttpClient(GoogleOAuthProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    CloseableHttpClient naverOAuthHttpClient(NaverOAuthProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    CloseableHttpClient kakaoOAuthHttpClient(KakaoOAuthProperties properties) {
        return pooledHttpClientFactory.create(properties);
    }

    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> socialOAuth2AccessTokenResponseClient(
            RestClient.Builder builder,
            @Qualifier("googleOAuthHttpClient") CloseableHttpClient googleHttpClient,
            @Qualifier("naverOAuthHttpClient") CloseableHttpClient naverHttpClient,
            @Qualifier("kakaoOAuthHttpClient") CloseableHttpClient kakaoHttpClient) {
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> google =
                tokenResponseClient(builder.clone(), googleHttpClient, false);
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> naver =
                tokenResponseClient(builder.clone(), naverHttpClient, true);
        OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> kakao =
                tokenResponseClient(builder.clone(), kakaoHttpClient, false);

        return request -> switch (request.getClientRegistration().getRegistrationId()) {
            case GOOGLE -> google.getTokenResponse(request);
            case NAVER -> naver.getTokenResponse(request);
            case KAKAO -> kakao.getTokenResponse(request);
            default -> throw new OAuth2AuthorizationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        };
    }

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> socialOAuth2UserService(
            RestTemplateBuilder builder,
            @Qualifier("naverOAuthHttpClient") CloseableHttpClient naverHttpClient,
            @Qualifier("kakaoOAuthHttpClient") CloseableHttpClient kakaoHttpClient) {
        DefaultOAuth2UserService naver = new DefaultOAuth2UserService();
        naver.setRestOperations(userInfoRestOperations(builder, naverHttpClient));
        naver.setAttributesConverter(request -> this::flattenNaverResponse);

        DefaultOAuth2UserService kakao = new DefaultOAuth2UserService();
        kakao.setRestOperations(userInfoRestOperations(builder, kakaoHttpClient));
        kakao.setAttributesConverter(request -> this::flattenKakaoResponse);

        return request -> switch (request.getClientRegistration().getRegistrationId()) {
            case NAVER -> naver.loadUser(request);
            case KAKAO -> kakao.loadUser(request);
            default -> throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST));
        };
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> googleOidcUserService(
            RestTemplateBuilder builder,
            @Qualifier("googleOAuthHttpClient") CloseableHttpClient httpClient) {
        DefaultOAuth2UserService userInfoService = new DefaultOAuth2UserService();
        userInfoService.setRestOperations(userInfoRestOperations(builder, httpClient));

        OidcUserService oidcUserService = new OidcUserService();
        oidcUserService.setOauth2UserService(userInfoService);
        return oidcUserService;
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient(
            RestClient.Builder builder, CloseableHttpClient httpClient, boolean includeState) {
        RestClientAuthorizationCodeTokenResponseClient client =
                new RestClientAuthorizationCodeTokenResponseClient();
        client.setRestClient(tokenRestClient(builder, httpClient));
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

    private RestClient tokenRestClient(RestClient.Builder builder, CloseableHttpClient httpClient) {
        return builder
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .configureMessageConverters(convertersBuilder -> convertersBuilder
                        .configureMessageConvertersList(converters -> {
                            converters.clear();
                            converters.add(new FormHttpMessageConverter());
                            converters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                        }))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
    }

    private RestOperations userInfoRestOperations(RestTemplateBuilder builder, CloseableHttpClient httpClient) {
        return builder
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(httpClient))
                .errorHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
    }

    private Map<String, Object> flattenNaverResponse(Map<String, Object> attributes) {
        Object response = attributes.get("response");
        if (!"00".equals(attributes.get("resultcode"))
                || !"success".equals(attributes.get("message"))
                || !(response instanceof Map<?, ?> profile)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response"));
        }

        Map<String, Object> flattened = new LinkedHashMap<>(attributes);
        copyStringAttributes(profile, flattened);
        return flattened;
    }

    Map<String, Object> flattenKakaoResponse(Map<String, Object> attributes) {
        Object account = attributes.get("kakao_account");
        if (!(account instanceof Map<?, ?> accountAttributes)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response"));
        }

        Map<String, Object> flattened = new LinkedHashMap<>(attributes);
        copyStringAttributes(accountAttributes, flattened);
        Object profile = accountAttributes.get("profile");
        if (profile instanceof Map<?, ?> profileAttributes) {
            copyStringAttributes(profileAttributes, flattened);
        }
        return flattened;
    }

    private void copyStringAttributes(Map<?, ?> source, Map<String, Object> target) {
        source.forEach((key, value) -> {
            if (key instanceof String attributeName) {
                target.put(attributeName, value);
            }
        });
    }
}
