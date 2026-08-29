package com.personal.happygallery.adapter.out.external.smartstore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverCommerceInventoryProvider implements SmartStoreInventoryProvider {

    private static final Logger log = LoggerFactory.getLogger(NaverCommerceInventoryProvider.class);
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 60;

    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final Clock clock;
    private volatile CachedToken cachedToken;

    public NaverCommerceInventoryProvider(
            RestClient smartStoreRestClient,
            SmartStoreProperties properties,
            Clock clock) {
        this.restClient = smartStoreRestClient;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public SyncResult sync(StockCommand command) {
        if (!properties.enabled()) {
            return SyncResult.failure("스마트스토어 재고 연동이 비활성 상태입니다.");
        }
        try {
            send(command, accessToken(false));
            return SyncResult.completed();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 401) {
                try {
                    send(command, accessToken(true));
                    return SyncResult.completed();
                } catch (RestClientResponseException retryException) {
                    return rejected(command, retryException);
                } catch (Exception retryException) {
                    return unavailable(command, retryException);
                }
            }
            return rejected(command, exception);
        } catch (Exception exception) {
            return unavailable(command, exception);
        }
    }

    private void send(StockCommand command, String accessToken) {
        if (command.optionProduct()) {
            restClient.put()
                    .uri("/external/v1/products/origin-products/{originProductNo}/option-stock",
                            command.originProductNo())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OptionStockRequest(new OptionInfo(
                            command.options().stream()
                                    .map(option -> new OptionCombination(
                                            option.optionId(), option.stockQuantity()))
                                    .toList(),
                            true)))
                    .retrieve()
                    .toBodilessEntity();
            return;
        }
        restClient.patch()
                .uri("/external/v1/products/origin-products/multi-update")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MultiUpdateRequest(List.of(new MultiUpdateItem(
                        command.originProductNo(), List.of("STOCK"), command.stockQuantity()))))
                .retrieve()
                .toBodilessEntity();
    }

    private synchronized String accessToken(boolean forceRefresh) {
        Instant now = clock.instant();
        CachedToken current = cachedToken;
        if (!forceRefresh && current != null
                && current.expiresAt().isAfter(now.plusSeconds(TOKEN_EXPIRY_MARGIN_SECONDS))) {
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

    private SyncResult rejected(StockCommand command, RestClientResponseException exception) {
        log.warn("스마트스토어 재고 반영 실패 [originProductNo={} status={}]",
                command.originProductNo(), exception.getStatusCode());
        return SyncResult.failure(exception.getStatusCode().is5xxServerError()
                || exception.getStatusCode().value() == 429
                ? "스마트스토어가 재고 요청을 처리하지 못했습니다."
                : "스마트스토어가 재고 요청을 거절했습니다.");
    }

    private SyncResult unavailable(StockCommand command, Exception exception) {
        log.warn("스마트스토어 재고 연동 통신 실패 [originProductNo={} type={}]",
                command.originProductNo(), exception.getClass().getSimpleName());
        return SyncResult.failure("스마트스토어에 연결하지 못했습니다.");
    }

    private record CachedToken(String value, Instant expiresAt) {}

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType
    ) {}

    private record OptionStockRequest(OptionInfo optionInfo) {}

    private record OptionInfo(List<OptionCombination> optionCombinations, boolean useStockManagement) {}

    private record OptionCombination(Long id, int stockQuantity) {}

    private record MultiUpdateRequest(List<MultiUpdateItem> multiProductUpdateRequestVos) {}

    private record MultiUpdateItem(
            Long originProductNo,
            List<String> multiUpdateTypes,
            int stockQuantity
    ) {}
}
