package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.SmartStoreOrderAttentionReason;
import com.personal.happygallery.domain.order.SmartStoreInventoryResolutionAction;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionStatus;
import com.personal.happygallery.domain.order.SmartStoreOrderReconciliationOutcome;
import com.personal.happygallery.application.shared.page.CursorPage;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreChannelOrderUseCase {

    CursorPage<ChannelOrderResult> list(
            boolean attentionOnly,
            SmartStoreOrderAttentionReason attentionReason,
            String cursor,
            int size);

    List<ReturnDeliveryCompanyResult> listReturnDeliveryCompanies();

    record ReturnDeliveryCompanyResult(long id, String name, String priorityType) {}

    ChannelOrderDetailResult detail(String productOrderId);

    ChannelOrderResult retryInventory(String productOrderId);

    ChannelOrderResult resolveReturn(String productOrderId, boolean restoreStock, String reviewVersion);

    ChannelOrderResult resolveInventory(InventoryResolutionCommand command, AdminActor actor);

    List<ActionHistoryResult> listActionHistory(String productOrderId);

    CursorPage<ActionHistoryResult> listUnresolvedActions(String cursor, int size);

    ActionHistoryResult reconcileAction(long historyId, ReconcileActionCommand command, AdminActor actor);

    CurrentOrderStatusResult currentStatus(String productOrderId);

    void confirm(String productOrderId, AdminActor actor);

    BulkOperationResult confirmAll(List<String> productOrderIds, AdminActor actor);

    void dispatch(DispatchCommand command, AdminActor actor);

    BulkOperationResult dispatchAll(List<DispatchCommand> commands, AdminActor actor);

    void delay(DelayCommand command, AdminActor actor);

    void approveCancel(String productOrderId, AdminActor actor);

    void approveReturn(String productOrderId, AdminActor actor);

    void rejectReturn(String productOrderId, AdminActor actor);

    void holdReturn(ReturnHoldCommand command, AdminActor actor);

    void releaseReturnHold(String productOrderId, AdminActor actor);

    void requestSellerReturn(SellerReturnCommand command, AdminActor actor);

    void dispatchExchange(ExchangeDispatchCommand command, AdminActor actor);

    void completeExchangeCollect(String productOrderId, AdminActor actor);

    void rejectExchange(ExchangeRejectCommand command, AdminActor actor);

    void holdExchange(ExchangeHoldCommand command, AdminActor actor);

    void releaseExchangeHold(String productOrderId, AdminActor actor);

    void requestSellerCancel(SellerCancelCommand command, AdminActor actor);

    record AdminActor(Long adminUserId, String name) {
        public AdminActor {
            name = name == null || name.isBlank() ? "시스템" : name.strip();
        }

        public static AdminActor system() {
            return new AdminActor(null, "시스템");
        }
    }

    record InventoryResolutionCommand(
            String productOrderId,
            Long productId,
            Long productVariantId,
            SmartStoreInventoryResolutionAction action,
            String reason,
            String expectedResolutionVersion
    ) {}

    record ReconcileActionCommand(
            SmartStoreOrderReconciliationOutcome outcome,
            String note
    ) {}

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
            LocalDateTime lastChangedAt,
            int pendingReturnQuantity,
            String returnReviewVersion,
            String inventoryResolutionVersion
    ) {}

    record ActionHistoryResult(
            long id,
            String productOrderId,
            SmartStoreOrderAction action,
            SmartStoreOrderActionStatus status,
            String requestSummary,
            String resultCode,
            String resultMessage,
            Long changedByAdminId,
            String changedBy,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            SmartStoreOrderReconciliationOutcome reconciliationOutcome,
            String reconciliationNote,
            Long reconciledByAdminId,
            String reconciledBy,
            LocalDateTime reconciledAt
    ) {}

    record CurrentOrderStatusResult(
            String productOrderId,
            String productOrderStatus,
            String placeOrderStatus,
            String claimType,
            String claimStatus,
            int remainQuantity,
            LocalDateTime shippingDueDate,
            String expectedDeliveryMethod,
            String deliveryCompany,
            String trackingNumber,
            ClaimDetail claimDetail
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
