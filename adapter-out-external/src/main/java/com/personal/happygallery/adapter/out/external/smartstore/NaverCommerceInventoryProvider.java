package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.product.port.out.SmartStoreInventoryProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.util.StringUtils;

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

    @Override
    public CatalogPage listProducts(int page, int size) {
        ProductSearchResponse response = accessTokenProvider.authorized(token -> restClient.post()
                .uri("/external/v1/products/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProductSearchRequest(page, size))
                .retrieve()
                .body(ProductSearchResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 상품 목록 응답이 비어 있습니다.");
        }
        List<CatalogProduct> products = response.contents() == null
                ? List.of()
                : response.contents().stream()
                        .flatMap(content -> content.channelProducts() == null
                                ? Stream.empty()
                                : content.channelProducts().stream()
                                        .flatMap(channel -> catalogProduct(content, channel).stream()))
                        .toList();
        return new CatalogPage(
                products, response.page(), response.size(),
                response.totalElements(), response.totalPages());
    }

    @Override
    public ChannelProduct getProduct(Long originProductNo) {
        ProductResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri("/external/v2/products/origin-products/{originProductNo}", originProductNo)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(ProductResponse.class));
        if (response == null || response.originProduct() == null) {
            throw new IllegalStateException("스마트스토어 원상품 응답이 비어 있습니다.");
        }
        RemoteOriginProduct product = response.originProduct();
        RemoteOptionInfo optionInfo = product.detailAttribute() == null
                ? null : product.detailAttribute().optionInfo();
        List<ChannelOption> options = optionInfo == null || optionInfo.optionCombinations() == null
                ? List.of()
                : optionInfo.optionCombinations().stream()
                        .map(option -> new ChannelOption(
                                option.id(), optionName(option), option.stockQuantity(),
                                option.price(), option.usable()))
                        .toList();
        return new ChannelProduct(product.salePrice(), product.statusType(), options);
    }

    @Override
    public SyncResult applyProduct(ProductCommand command) {
        if (!properties.enabled()) {
            return SyncResult.failure("스마트스토어 상품 연동이 비활성 상태입니다.");
        }
        try {
            accessTokenProvider.authorized(token -> {
                if (command.options().isEmpty()) {
                    updatePriceAndStock(command, token);
                } else {
                    updateOptions(command, token);
                }
                if (!("OUTOFSTOCK".equals(command.targetStatus())
                        && !command.options().isEmpty())) {
                    updateStatus(command, token);
                }
                return null;
            });
            return SyncResult.completed();
        } catch (RestClientResponseException exception) {
            return SyncResult.failure(exception.getStatusCode().is5xxServerError()
                    || exception.getStatusCode().value() == 429
                    ? "스마트스토어가 상품 반영 요청을 처리하지 못했습니다."
                    : "스마트스토어가 상품 반영 요청을 거절했습니다.");
        } catch (Exception exception) {
            return SyncResult.failure("스마트스토어에 연결하지 못했습니다.");
        }
    }

    private void updatePriceAndStock(ProductCommand command, String token) {
        restClient.patch()
                .uri("/external/v1/products/origin-products/multi-update")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new MultiUpdateRequest(List.of(new MultiUpdateItem(
                        command.originProductNo(), List.of("SALE_PRICE", "STOCK"),
                        command.stockQuantity(), new ProductSalePrice(command.salePrice())))))
                .retrieve()
                .toBodilessEntity();
    }

    private void updateOptions(ProductCommand command, String token) {
        restClient.put()
                .uri("/external/v1/products/origin-products/{originProductNo}/option-stock",
                        command.originProductNo())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProductOptionUpdateRequest(
                        new ProductSalePrice(command.salePrice()),
                        new ProductOptionInfo(command.options().stream()
                                .map(option -> new ProductOptionCombination(
                                        option.optionId(), option.stockQuantity(), option.price(),
                                        option.usable()))
                                .toList(), true)))
                .retrieve()
                .toBodilessEntity();
    }

    private void updateStatus(ProductCommand command, String token) {
        restClient.put()
                .uri("/external/v1/products/origin-products/{originProductNo}/change-status",
                        command.originProductNo())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ProductStatusRequest(
                        command.targetStatus(),
                        "SALE".equals(command.targetStatus()) ? command.stockQuantity() : null))
                .retrieve()
                .toBodilessEntity();
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
                        command.originProductNo(), List.of("STOCK"), command.stockQuantity(), null))))
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

    private static Optional<CatalogProduct> catalogProduct(
            ProductSearchContent content, RemoteChannelProduct channel) {
        if (!"STOREFARM".equals(channel.channelServiceType())) {
            return Optional.empty();
        }
        Long originProductNo = channel.originProductNo() == null
                ? content.originProductNo() : channel.originProductNo();
        if (originProductNo == null) {
            return Optional.empty();
        }
        return Optional.of(new CatalogProduct(
                originProductNo,
                Objects.requireNonNullElse(channel.name(), "상품명 없음"),
                Objects.requireNonNullElse(channel.statusType(), "UNKNOWN"),
                channel.salePrice(), channel.stockQuantity(),
                channel.representativeImage() == null ? null : channel.representativeImage().url()));
    }

    private static String optionName(RemoteOptionCombination option) {
        String name = Stream.of(
                        option.optionName1(), option.optionName2(),
                        option.optionName3(), option.optionName4())
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" / "));
        return name.isEmpty() ? "옵션 " + option.id() : name;
    }

    private record OptionStockRequest(OptionInfo optionInfo) {}

    private record OptionInfo(List<OptionCombination> optionCombinations, boolean useStockManagement) {}

    private record OptionCombination(Long id, int stockQuantity) {}

    private record MultiUpdateRequest(List<MultiUpdateItem> multiProductUpdateRequestVos) {}

    private record MultiUpdateItem(
            Long originProductNo,
            List<String> multiUpdateTypes,
            Integer stockQuantity,
            ProductSalePrice productSalePrice
    ) {}

    private record ProductSalePrice(long salePrice) {}

    private record ProductOptionUpdateRequest(
            ProductSalePrice productSalePrice,
            ProductOptionInfo optionInfo
    ) {}

    private record ProductOptionInfo(
            List<ProductOptionCombination> optionCombinations,
            boolean useStockManagement
    ) {}

    private record ProductOptionCombination(
            Long id,
            int stockQuantity,
            long price,
            boolean usable
    ) {}

    private record ProductStatusRequest(String statusType, Integer stockQuantity) {}

    private record ProductSearchRequest(int page, int size) {}

    private record ProductSearchResponse(
            List<ProductSearchContent> contents,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    private record ProductSearchContent(
            Long originProductNo,
            List<RemoteChannelProduct> channelProducts
    ) {}

    private record RemoteChannelProduct(
            Long originProductNo,
            String channelServiceType,
            String name,
            String statusType,
            long salePrice,
            Integer stockQuantity,
            RemoteImage representativeImage
    ) {}

    private record RemoteImage(String url) {}

    private record ProductResponse(RemoteOriginProduct originProduct) {}

    private record RemoteOriginProduct(
            long salePrice,
            String statusType,
            RemoteDetailAttribute detailAttribute
    ) {}

    private record RemoteDetailAttribute(RemoteOptionInfo optionInfo) {}

    private record RemoteOptionInfo(List<RemoteOptionCombination> optionCombinations) {}

    private record RemoteOptionCombination(
            Long id,
            String optionName1,
            String optionName2,
            String optionName3,
            String optionName4,
            int stockQuantity,
            long price,
            boolean usable
    ) {}
}
