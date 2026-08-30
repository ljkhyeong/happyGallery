package com.personal.happygallery.adapter.out.external.smartstore;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.function.Function;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
class NaverCommerceAccessTokenProvider {

    private static final long EXPIRY_MARGIN_SECONDS = 60;

    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final Clock clock;
    private volatile CachedToken cachedToken;

    NaverCommerceAccessTokenProvider(
            RestClient smartStoreRestClient,
            SmartStoreProperties properties,
            Clock clock) {
        this.restClient = smartStoreRestClient;
        this.properties = properties;
        this.clock = clock;
    }

    synchronized String accessToken(boolean forceRefresh) {
        Instant now = clock.instant();
        CachedToken current = cachedToken;
        if (!forceRefresh && current != null
                && current.expiresAt().isAfter(now.plusSeconds(EXPIRY_MARGIN_SECONDS))) {
            return current.value();
        }

        long timestamp = now.toEpochMilli();
        String password = properties.clientId() + "_" + timestamp;
        String hashed = BCrypt.hashpw(password, properties.clientSecret());
        String signature = Base64.getUrlEncoder()
                .encodeToString(hashed.getBytes(StandardCharsets.UTF_8));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("timestamp", Long.toString(timestamp));
        form.add("grant_type", "client_credentials");
        form.add("client_secret_sign", signature);
        form.add("type", properties.accountType());
        if (StringUtils.hasText(properties.accountId())) {
            form.add("account_id", properties.accountId());
        }
        TokenResponse response = restClient.post()
                .uri("/external/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (response == null || !StringUtils.hasText(response.accessToken()) || response.expiresIn() < 1) {
            throw new IllegalStateException("스마트스토어 인증 토큰 응답이 비어 있습니다.");
        }
        cachedToken = new CachedToken(response.accessToken(), now.plusSeconds(response.expiresIn()));
        return response.accessToken();
    }

    <T> T authorized(Function<String, T> request) {
        try {
            return request.apply(accessToken(false));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 401) {
                throw exception;
            }
            return request.apply(accessToken(true));
        }
    }

    private record CachedToken(String value, Instant expiresAt) {}

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {}
}
