package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.order.dto.SmartStoreClaimDetail;
import com.personal.happygallery.application.order.dto.SmartStoreDelayCommand;
import com.personal.happygallery.application.order.dto.SmartStoreDeliveryInfo;
import com.personal.happygallery.application.order.dto.SmartStoreDispatchCommand;
import com.personal.happygallery.application.order.dto.SmartStoreExchangeDispatchCommand;
import com.personal.happygallery.application.order.dto.SmartStoreExchangeHoldCommand;
import com.personal.happygallery.application.order.dto.SmartStoreExchangeRejectCommand;
import com.personal.happygallery.application.order.dto.SmartStoreOperationResult;
import com.personal.happygallery.application.order.dto.SmartStoreReturnHoldCommand;
import com.personal.happygallery.application.order.dto.SmartStoreSellerCancelCommand;
import com.personal.happygallery.application.order.dto.SmartStoreSellerReturnCommand;
import com.personal.happygallery.application.shared.AdminActor;
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

    SmartStoreOperationResult confirmAll(List<String> productOrderIds, AdminActor actor);

    void dispatch(SmartStoreDispatchCommand command, AdminActor actor);

    SmartStoreOperationResult dispatchAll(List<SmartStoreDispatchCommand> commands, AdminActor actor);

    void delay(SmartStoreDelayCommand command, AdminActor actor);

    void approveCancel(String productOrderId, AdminActor actor);

    void approveReturn(String productOrderId, AdminActor actor);

    void rejectReturn(String productOrderId, AdminActor actor);

    void holdReturn(SmartStoreReturnHoldCommand command, AdminActor actor);

    void releaseReturnHold(String productOrderId, AdminActor actor);

    void requestSellerReturn(SmartStoreSellerReturnCommand command, AdminActor actor);

    void dispatchExchange(SmartStoreExchangeDispatchCommand command, AdminActor actor);

    void completeExchangeCollect(String productOrderId, AdminActor actor);

    void rejectExchange(SmartStoreExchangeRejectCommand command, AdminActor actor);

    void holdExchange(SmartStoreExchangeHoldCommand command, AdminActor actor);

    void releaseExchangeHold(String productOrderId, AdminActor actor);

    void requestSellerCancel(SmartStoreSellerCancelCommand command, AdminActor actor);

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
            SmartStoreClaimDetail claimDetail
    ) {}

    record ChannelOrderDetailResult(
            ChannelOrderResult order,
            SmartStoreDeliveryInfo deliveryInfo,
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
            SmartStoreClaimDetail claimDetail
    ) {}
}
