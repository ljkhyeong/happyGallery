package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreChannelOrderUseCase {

    List<ChannelOrderResult> list(boolean attentionOnly, int limit);

    ChannelOrderDetailResult detail(String productOrderId);

    ChannelOrderResult retryInventory(String productOrderId);

    ChannelOrderResult resolveReturn(String productOrderId, boolean restoreStock);

    void confirm(String productOrderId);

    void dispatch(DispatchCommand command);

    void delay(DelayCommand command);

    void approveCancel(String productOrderId);

    void approveReturn(String productOrderId);

    void rejectReturn(String productOrderId);

    void dispatchExchange(ExchangeDispatchCommand command);

    void completeExchangeCollect(String productOrderId);

    void rejectExchange(ExchangeRejectCommand command);

    void holdExchange(ExchangeHoldCommand command);

    void releaseExchangeHold(String productOrderId);

    void requestSellerCancel(SellerCancelCommand command);

    record ChannelOrderResult(
            String productOrderId,
            String orderId,
            Long originProductNo,
            Long itemNo,
            Long productId,
            Long productVariantId,
            String productName,
            String productOption,
            String productOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity,
            int inventoryAppliedQuantity,
            SmartStoreOrderAttentionReason attentionReason,
            LocalDateTime paymentDate,
            LocalDateTime lastChangedAt
    ) {}

    record ChannelOrderDetailResult(
            ChannelOrderResult order,
            DeliveryInfo deliveryInfo,
            String placeOrderStatus,
            LocalDateTime shippingDueDate,
            String expectedDeliveryMethod,
            String deliveryCompany,
            String trackingNumber,
            Long unitPrice,
            Long paymentAmount,
            Long paymentCommission,
            Long saleCommission,
            Long channelCommission,
            Long expectedSettlementAmount,
            ClaimDetail claimDetail
    ) {}

    record ClaimDetail(
            String claimId,
            String claimType,
            String claimStatus,
            String reason,
            String detailedReason,
            Integer requestQuantity,
            LocalDateTime requestedAt,
            String collectStatus,
            String collectDeliveryCompany,
            String collectTrackingNumber,
            Long claimDeliveryFeeDemandAmount,
            String holdbackStatus,
            List<String> imageUrls
    ) {}

    record DeliveryInfo(
            String recipientName,
            String phone,
            String postalCode,
            String addressLine1,
            String addressLine2,
            String shippingMemo
    ) {}

    record DispatchCommand(
            String productOrderId,
            String deliveryMethod,
            String deliveryCompanyCode,
            String trackingNumber,
            LocalDateTime dispatchDate
    ) {}

    record DelayCommand(
            String productOrderId,
            LocalDateTime dispatchDueDate,
            String reasonCode,
            String detailedReason
    ) {}

    record ExchangeDispatchCommand(
            String productOrderId,
            String deliveryMethod,
            String deliveryCompanyCode,
            String trackingNumber
    ) {}

    record ExchangeRejectCommand(String productOrderId, String reason) {}

    record ExchangeHoldCommand(
            String productOrderId,
            String holdbackClassType,
            String detailedReason,
            Long extraExchangeFeeAmount
    ) {}

    record SellerCancelCommand(
            String productOrderId,
            String reason,
            String detailedReason,
            Integer quantity
    ) {}
}
