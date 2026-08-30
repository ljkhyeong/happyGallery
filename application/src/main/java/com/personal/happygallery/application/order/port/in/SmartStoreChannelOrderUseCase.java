package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreChannelOrderUseCase {

    List<ChannelOrderResult> list(boolean attentionOnly, int limit);

    List<ReturnDeliveryCompanyResult> listReturnDeliveryCompanies();

    record ReturnDeliveryCompanyResult(long id, String name, String priorityType) {}

    ChannelOrderDetailResult detail(String productOrderId);

    ChannelOrderResult retryInventory(String productOrderId);

    ChannelOrderResult resolveReturn(String productOrderId, boolean restoreStock);

    void confirm(String productOrderId);

    BulkOperationResult confirmAll(List<String> productOrderIds);

    void dispatch(DispatchCommand command);

    BulkOperationResult dispatchAll(List<DispatchCommand> commands);

    void delay(DelayCommand command);

    void approveCancel(String productOrderId);

    void approveReturn(String productOrderId);

    void rejectReturn(String productOrderId);

    void holdReturn(ReturnHoldCommand command);

    void releaseReturnHold(String productOrderId);

    void requestSellerReturn(SellerReturnCommand command);

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

    record ReturnHoldCommand(
            String productOrderId,
            String holdbackClassType,
            String detailedReason,
            Long extraReturnFeeAmount
    ) {}

    record SellerReturnCommand(
            String productOrderId,
            String returnReason,
            String collectDeliveryMethod,
            String collectDeliveryCompany,
            String collectTrackingNumber,
            Integer returnQuantity
    ) {}

    record SellerCancelCommand(
            String productOrderId,
            String reason,
            String detailedReason,
            Integer quantity
    ) {}

    record BulkOperationResult(
            List<String> successProductOrderIds,
            List<BulkOperationFailure> failures
    ) {}

    record BulkOperationFailure(String productOrderId, String code, String message) {}
}
