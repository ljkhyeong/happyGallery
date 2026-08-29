package com.personal.happygallery.adapter.out.external.smartstore;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.domain.time.Clocks;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class NaverCommerceOrderProvider implements SmartStoreOrderProvider {

    private static final int PAGE_SIZE = 300;

    private final RestClient restClient;
    private final SmartStoreProperties properties;
    private final NaverCommerceAccessTokenProvider accessTokenProvider;

    public NaverCommerceOrderProvider(
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
    public ChangePage fetchChanges(ChangeCursor cursor, LocalDateTime changedTo) {
        ChangedResponse response = authorized(token -> restClient.get()
                .uri(builder -> {
                    builder.path("/external/v1/pay-order/seller/product-orders/last-changed-statuses")
                            .queryParam("lastChangedFrom", format(cursor.changedFrom()))
                            .queryParam("lastChangedTo", format(changedTo))
                            .queryParam("limitCount", PAGE_SIZE);
                    if (StringUtils.hasText(cursor.moreSequence())) {
                        builder.queryParam("moreSequence", cursor.moreSequence());
                    }
                    return builder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(ChangedResponse.class));
        if (response == null || response.data() == null) {
            throw new IllegalStateException("스마트스토어 변경 주문 응답이 비어 있습니다.");
        }
        List<ProductOrderChange> changes = response.data().lastChangeStatuses() == null
                ? List.of()
                : response.data().lastChangeStatuses().stream()
                        .map(change -> new ProductOrderChange(
                                change.productOrderId(),
                                change.lastChangedType(),
                                toLocalDateTime(change.lastChangedDate())))
                        .toList();
        More more = response.data().more();
        ChangeCursor next = more == null
                ? null
                : new ChangeCursor(toLocalDateTime(more.moreFrom()), more.moreSequence());
        return new ChangePage(changes, next);
    }

    @Override
    public List<ProductOrderDetail> fetchDetails(List<String> productOrderIds) {
        if (productOrderIds.isEmpty()) {
            return List.of();
        }
        DetailResponse response = authorized(token -> restClient.post()
                .uri("/external/v1/pay-order/seller/product-orders/query")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DetailRequest(productOrderIds, true))
                .retrieve()
                .body(DetailResponse.class));
        if (response == null || response.data() == null) {
            throw new IllegalStateException("스마트스토어 상품 주문 상세 응답이 비어 있습니다.");
        }
        return response.data().stream().map(item -> {
            OrderInfo order = item.order();
            ProductOrderInfo productOrder = item.productOrder();
            if (order == null || productOrder == null) {
                throw new IllegalStateException("스마트스토어 상품 주문 상세 항목이 비어 있습니다.");
            }
            return new ProductOrderDetail(
                    productOrder.productOrderId(),
                    order.orderId(),
                    parseRequiredLong(productOrder.originalProductId()),
                    parseNullableLong(productOrder.itemNo()),
                    productOrder.productName(),
                    productOrder.productOption(),
                    productOrder.productOrderStatus(),
                    productOrder.claimType(),
                    productOrder.claimStatus(),
                    productOrder.initialQuantity(),
                    productOrder.remainQuantity(),
                    toNullableLocalDateTime(order.paymentDate()));
        }).toList();
    }

    private <T> T authorized(Function<String, T> request) {
        try {
            return request.apply(accessTokenProvider.accessToken(false));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 401) {
                throw exception;
            }
            return request.apply(accessTokenProvider.accessToken(true));
        }
    }

    private static String format(LocalDateTime value) {
        return value.atZone(Clocks.SEOUL).toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static LocalDateTime toLocalDateTime(OffsetDateTime value) {
        if (value == null) {
            throw new IllegalStateException("스마트스토어 주문 변경 일시가 비어 있습니다.");
        }
        return value.atZoneSameInstant(Clocks.SEOUL).toLocalDateTime();
    }

    private static LocalDateTime toNullableLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.atZoneSameInstant(Clocks.SEOUL).toLocalDateTime();
    }

    private static Long parseRequiredLong(String value) {
        Long parsed = parseNullableLong(value);
        if (parsed == null) {
            throw new IllegalStateException("스마트스토어 원상품 번호가 비어 있습니다.");
        }
        return parsed;
    }

    private static Long parseNullableLong(String value) {
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private record ChangedResponse(ChangedData data) {}

    private record ChangedData(List<ChangedOrder> lastChangeStatuses, More more) {}

    private record ChangedOrder(
            String productOrderId,
            String lastChangedType,
            OffsetDateTime lastChangedDate
    ) {}

    private record More(OffsetDateTime moreFrom, String moreSequence) {}

    private record DetailRequest(
            List<String> productOrderIds,
            boolean quantityClaimCompatibility
    ) {}

    private record DetailResponse(List<DetailItem> data) {}

    private record DetailItem(OrderInfo order, ProductOrderInfo productOrder) {}

    private record OrderInfo(
            String orderId,
            OffsetDateTime paymentDate
    ) {}

    private record ProductOrderInfo(
            String productOrderId,
            String originalProductId,
            String itemNo,
            String productName,
            String productOption,
            String productOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity
    ) {}
}
