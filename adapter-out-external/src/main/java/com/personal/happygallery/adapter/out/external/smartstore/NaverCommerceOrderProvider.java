package com.personal.happygallery.adapter.out.external.smartstore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider;
import com.personal.happygallery.domain.time.Clocks;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

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
    public List<ReturnDeliveryCompany> findReturnDeliveryCompanies() {
        ReturnDeliveryCompaniesResponse response = accessTokenProvider.authorized(token -> restClient.get()
                .uri("/external/v2/product-delivery-info/return-delivery-companies")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(ReturnDeliveryCompaniesResponse.class));
        if (response == null) {
            throw new IllegalStateException("스마트스토어 반품 택배사 응답이 비어 있습니다.");
        }
        return response.returnDeliveryCompanies() == null ? List.of() : response.returnDeliveryCompanies();
    }

    private record ReturnDeliveryCompaniesResponse(List<ReturnDeliveryCompany> returnDeliveryCompanies) {}

    @Override
    public ChangePage fetchChanges(ChangeCursor cursor, LocalDateTime changedTo) {
        ChangedResponse response = accessTokenProvider.authorized(token -> restClient.get()
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
        DetailResponse response = accessTokenProvider.authorized(token -> restClient.post()
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
            ShippingAddressInfo address = productOrder.shippingAddress();
            DeliveryInfo deliveryInfo = address == null ? null : new DeliveryInfo(
                    address.name(), address.tel1(), address.zipCode(), address.baseAddress(),
                    address.detailedAddress(), productOrder.shippingMemo());
            DeliveryResponse delivery = item.delivery();
            return new ProductOrderDetail(
                    productOrder.productOrderId(),
                    order.orderId(),
                    parseRequiredLong(productOrder.originalProductId()),
                    parseNullableLong(productOrder.itemNo()),
                    productOrder.productName(),
                    productOrder.productOption(),
                    deliveryInfo,
                    productOrder.productOrderStatus(),
                    productOrder.placeOrderStatus(),
                    productOrder.claimType(),
                    productOrder.claimStatus(),
                    claimDetail(item.currentClaim()),
                    productOrder.initialQuantity(),
                    productOrder.remainQuantity(),
                    toNullableLocalDateTime(order.paymentDate()),
                    toNullableLocalDateTime(productOrder.shippingDueDate()),
                    productOrder.expectedDeliveryMethod(),
                    delivery == null ? null : delivery.deliveryCompany(),
                    delivery == null ? null : delivery.trackingNumber(),
                    productOrder.unitPrice(),
                    productOrder.remainPaymentAmount(),
                    productOrder.paymentCommission(),
                    productOrder.saleCommission(),
                    productOrder.channelCommission(),
                    productOrder.expectedSettlementAmount());
        }).toList();
    }

    @Override
    public OperationResult confirmAll(List<String> productOrderIds) {
        OperationResponse response = executeBulk(
                "/external/v1/pay-order/seller/product-orders/confirm",
                new ConfirmRequest(productOrderIds));
        return operationResult(response, productOrderIds);
    }

    @Override
    public OperationResult dispatchAll(List<DispatchCommand> commands) {
        OperationResponse response = executeBulk(
                "/external/v1/pay-order/seller/product-orders/dispatch",
                new DispatchRequest(commands.stream()
                        .map(command -> new DispatchItem(
                                command.productOrderId(), command.deliveryMethod(),
                                command.deliveryCompanyCode(), command.trackingNumber(),
                                format(command.dispatchDate())))
                        .toList()));
        return operationResult(response, commands.stream()
                .map(DispatchCommand::productOrderId)
                .toList());
    }

    @Override
    public void delay(DelayCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/delay",
                new DelayRequest(
                        format(command.dispatchDueDate()), command.reasonCode(),
                        command.detailedReason()), command.productOrderId());
    }

    @Override
    public void approveCancel(String productOrderId) {
        executeWithoutBody("/external/v1/pay-order/seller/product-orders/"
                + productOrderId + "/claim/cancel/approve", productOrderId);
    }

    @Override
    public void approveReturn(String productOrderId) {
        executeWithoutBody("/external/v1/pay-order/seller/product-orders/"
                + productOrderId + "/claim/return/approve", productOrderId);
    }

    @Override
    public void rejectReturn(String productOrderId) {
        executeWithoutBody("/external/v1/pay-order/seller/product-orders/"
                + productOrderId + "/claim/return/reject", productOrderId);
    }

    @Override
    public void holdReturn(ReturnHoldCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/claim/return/holdback",
                new ReturnHoldRequest(command.holdbackClassType(), command.detailedReason(),
                        command.extraReturnFeeAmount()), command.productOrderId());
    }

    @Override
    public void releaseReturnHold(String productOrderId) {
        executeWithoutBody("/external/v1/pay-order/seller/product-orders/"
                + productOrderId + "/claim/return/holdback/release", productOrderId);
    }

    @Override
    public void requestSellerReturn(SellerReturnCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/claim/return/request",
                new SellerReturnRequest(
                        command.returnReason(), command.collectDeliveryMethod(),
                        command.collectDeliveryCompany(), command.collectTrackingNumber(),
                        command.returnQuantity()), command.productOrderId());
    }

    @Override
    public void dispatchExchange(ExchangeDispatchCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/claim/exchange/dispatch",
                new ExchangeDispatchRequest(
                        command.deliveryMethod(), command.deliveryCompanyCode(),
                        command.trackingNumber()), command.productOrderId());
    }

    @Override
    public void completeExchangeCollect(String productOrderId) {
        executeWithoutBody("/external/v1/pay-order/seller/product-orders/"
                + productOrderId + "/claim/exchange/collect/approve", productOrderId);
    }

    @Override
    public void rejectExchange(ExchangeRejectCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/claim/exchange/reject",
                new ExchangeRejectRequest(command.reason()), command.productOrderId());
    }

    @Override
    public void holdExchange(ExchangeHoldCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/claim/exchange/holdback",
                new ExchangeHoldRequest(command.holdbackClassType(), command.detailedReason(),
                        command.extraExchangeFeeAmount()), command.productOrderId());
    }

    @Override
    public void releaseExchangeHold(String productOrderId) {
        executeWithoutBody("/external/v1/pay-order/seller/product-orders/"
                + productOrderId + "/claim/exchange/holdback/release", productOrderId);
    }

    @Override
    public void requestSellerCancel(SellerCancelCommand command) {
        execute("/external/v1/pay-order/seller/product-orders/"
                        + command.productOrderId() + "/claim/cancel/request",
                new SellerCancelRequest(command.reason(), command.detailedReason(),
                        command.quantity()), command.productOrderId());
    }

    private void execute(String path, Object body, String productOrderId) {
        OperationResponse response = accessTokenProvider.authorized(token -> restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OperationResponse.class));
        requireOperationSuccess(response, productOrderId);
    }

    private OperationResponse executeBulk(String path, Object body) {
        return accessTokenProvider.authorized(token -> restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OperationResponse.class));
    }

    private void executeWithoutBody(String path, String productOrderId) {
        OperationResponse response = accessTokenProvider.authorized(token -> restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentLength(0)
                .retrieve()
                .body(OperationResponse.class));
        requireOperationSuccess(response, productOrderId);
    }

    private static void requireOperationSuccess(OperationResponse response, String productOrderId) {
        if (response == null || response.data() == null) {
            return;
        }
        List<RemoteOperationFailure> failures = response.data().failProductOrderInfos();
        if (failures == null || failures.isEmpty()) {
            return;
        }
        RemoteOperationFailure failure = failures.stream()
                .filter(item -> productOrderId.equals(item.productOrderId()))
                .findFirst()
                .orElse(failures.getFirst());
        throw new IllegalStateException("스마트스토어 주문 처리 실패: " + failure.message());
    }

    private static OperationResult operationResult(
            OperationResponse response, List<String> requestedIds) {
        OperationData data = response == null ? null : response.data();
        if (data == null) {
            return new OperationResult(requestedIds, List.of());
        }
        List<String> successIds = data.successProductOrderIds() == null
                ? List.of() : data.successProductOrderIds();
        if (successIds.isEmpty() && data.successProductOrderInfos() != null) {
            successIds = data.successProductOrderInfos().stream()
                    .map(OperationSuccess::productOrderId)
                    .toList();
        }
        List<OperationFailure> failures = data.failProductOrderInfos() == null
                ? List.of() : data.failProductOrderInfos().stream()
                        .map(failure -> new OperationFailure(
                                failure.productOrderId(), failure.code(), failure.message()))
                        .toList();
        List<String> completedIds = successIds;
        List<String> failedIds = failures.stream()
                .map(OperationFailure::productOrderId)
                .toList();
        List<OperationFailure> missing = requestedIds.stream()
                .filter(id -> !completedIds.contains(id) && !failedIds.contains(id))
                .map(id -> new OperationFailure(
                        id, "UNKNOWN_RESULT", "네이버 응답에서 처리 결과를 확인할 수 없습니다."))
                .toList();
        if (!missing.isEmpty()) {
            failures = java.util.stream.Stream.concat(failures.stream(), missing.stream()).toList();
        }
        return new OperationResult(successIds, failures);
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

    private static ClaimDetail claimDetail(CurrentClaim currentClaim) {
        if (currentClaim == null) {
            return null;
        }
        if (currentClaim.cancel() != null) {
            CancelClaim claim = currentClaim.cancel();
            return new ClaimDetail(
                    claim.claimId(), "CANCEL", claim.claimStatus(), claim.cancelReason(),
                    claim.cancelDetailedReason(), claim.requestQuantity(),
                    toNullableLocalDateTime(claim.claimRequestDate()), null, null, null,
                    null, null, List.of());
        }
        if (currentClaim.returned() != null) {
            ReturnClaim claim = currentClaim.returned();
            return new ClaimDetail(
                    claim.claimId(), "RETURN", claim.claimStatus(), claim.returnReason(),
                    claim.returnDetailedReason(), claim.requestQuantity(),
                    toNullableLocalDateTime(claim.claimRequestDate()), claim.collectStatus(),
                    claim.collectDeliveryCompany(), claim.collectTrackingNumber(),
                    claim.claimDeliveryFeeDemandAmount(), claim.holdbackStatus(),
                    claim.returnImageUrl());
        }
        if (currentClaim.exchange() != null) {
            ExchangeClaim claim = currentClaim.exchange();
            return new ClaimDetail(
                    claim.claimId(), "EXCHANGE", claim.claimStatus(), claim.exchangeReason(),
                    claim.exchangeDetailedReason(), claim.requestQuantity(),
                    toNullableLocalDateTime(claim.claimRequestDate()), claim.collectStatus(),
                    claim.collectDeliveryCompany(), claim.collectTrackingNumber(),
                    claim.claimDeliveryFeeDemandAmount(), claim.holdbackStatus(),
                    claim.exchangeImageUrl());
        }
        return null;
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

    private record DetailItem(
            OrderInfo order,
            ProductOrderInfo productOrder,
            DeliveryResponse delivery,
            CurrentClaim currentClaim
    ) {}

    private record CurrentClaim(
            CancelClaim cancel,
            @JsonProperty("return") ReturnClaim returned,
            ExchangeClaim exchange
    ) {}

    private record CancelClaim(
            String claimId,
            String claimStatus,
            OffsetDateTime claimRequestDate,
            Integer requestQuantity,
            String cancelReason,
            String cancelDetailedReason
    ) {}

    private record ReturnClaim(
            String claimId,
            String claimStatus,
            OffsetDateTime claimRequestDate,
            Integer requestQuantity,
            String returnReason,
            String returnDetailedReason,
            String collectStatus,
            String collectDeliveryCompany,
            String collectTrackingNumber,
            Long claimDeliveryFeeDemandAmount,
            String holdbackStatus,
            List<String> returnImageUrl
    ) {}

    private record ExchangeClaim(
            String claimId,
            String claimStatus,
            OffsetDateTime claimRequestDate,
            Integer requestQuantity,
            String exchangeReason,
            String exchangeDetailedReason,
            String collectStatus,
            String collectDeliveryCompany,
            String collectTrackingNumber,
            Long claimDeliveryFeeDemandAmount,
            String holdbackStatus,
            List<String> exchangeImageUrl
    ) {}

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
            ShippingAddressInfo shippingAddress,
            String shippingMemo,
            OffsetDateTime shippingDueDate,
            String expectedDeliveryMethod,
            String placeOrderStatus,
            String productOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity,
            Long unitPrice,
            Long remainPaymentAmount,
            Long paymentCommission,
            Long saleCommission,
            Long channelCommission,
            Long expectedSettlementAmount
    ) {}

    private record ShippingAddressInfo(
            String name,
            String tel1,
            String zipCode,
            String baseAddress,
            String detailedAddress
    ) {}

    private record DeliveryResponse(String deliveryCompany, String trackingNumber) {}

    private record ConfirmRequest(List<String> productOrderIds) {}

    private record DispatchRequest(List<DispatchItem> dispatchProductOrders) {}

    private record DispatchItem(
            String productOrderId,
            String deliveryMethod,
            String deliveryCompanyCode,
            String trackingNumber,
            String dispatchDate
    ) {}

    private record DelayRequest(
            String dispatchDueDate,
            String delayedDispatchReason,
            String dispatchDelayedDetailedReason
    ) {}

    private record ExchangeDispatchRequest(
            String reDeliveryMethod,
            String reDeliveryCompany,
            String reDeliveryTrackingNumber
    ) {}

    private record ExchangeRejectRequest(String rejectExchangeReason) {}

    private record ExchangeHoldRequest(
            String holdbackClassType,
            String holdbackExchangeDetailReason,
            Long extraExchangeFeeAmount
    ) {}

    private record ReturnHoldRequest(
            String holdbackClassType,
            String holdbackReturnDetailReason,
            Long extraReturnFeeAmount
    ) {}

    private record SellerReturnRequest(
            String returnReason,
            String collectDeliveryMethod,
            String collectDeliveryCompany,
            String collectTrackingNumber,
            Integer returnQuantity
    ) {}

    private record SellerCancelRequest(
            String cancelReason,
            String cancelDetailedReason,
            Integer cancelQuantity
    ) {}

    private record OperationResponse(OperationData data) {}

    private record OperationData(
            List<String> successProductOrderIds,
            List<OperationSuccess> successProductOrderInfos,
            List<RemoteOperationFailure> failProductOrderInfos
    ) {}

    private record OperationSuccess(String productOrderId) {}

    private record RemoteOperationFailure(String productOrderId, String code, String message) {}
}
