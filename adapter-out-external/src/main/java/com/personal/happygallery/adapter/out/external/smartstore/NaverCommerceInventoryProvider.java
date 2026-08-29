package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverCommerceInventoryProvider implements SmartStoreInventoryProvider {

    private static final Logger log = LoggerFactory.getLogger(NaverCommerceInventoryProvider.class);
    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final NaverCommerceAccessTokenProvider accessTokenProvider;

    public NaverCommerceInventoryProvider(
            RestClient smartStoreRestClient,
            SmartStoreProperties properties,
            NaverCommerceAccessTokenProvider accessTokenProvider) {
        this.restClient = smartStoreRestClient;
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
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
            accessTokenProvider.authorized(token -> {
                send(command, token);
                return null;
            });
            return SyncResult.completed();
        } catch (RestClientResponseException exception) {
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
