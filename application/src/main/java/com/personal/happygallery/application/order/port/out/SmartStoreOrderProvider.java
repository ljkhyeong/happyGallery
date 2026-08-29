package com.personal.happygallery.application.order.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreOrderProvider {

    boolean isEnabled();

    ChangePage fetchChanges(ChangeCursor cursor, LocalDateTime changedTo);

    List<ProductOrderDetail> fetchDetails(List<String> productOrderIds);

    void confirm(String productOrderId);

    void dispatch(DispatchCommand command);

    void delay(DelayCommand command);

    void approveCancel(String productOrderId);

    void approveReturn(String productOrderId);

    void rejectReturn(String productOrderId);

    void dispatchExchange(ExchangeDispatchCommand command);

    record ChangeCursor(LocalDateTime changedFrom, String moreSequence) {}

    record ChangePage(List<ProductOrderChange> changes, ChangeCursor nextCursor) {
        public ChangePage {
            changes = List.copyOf(changes);
        }
    }

    record ProductOrderChange(
            String productOrderId,
            String lastChangedType,
            LocalDateTime lastChangedAt
    ) {}

    record ProductOrderDetail(
            String productOrderId,
            String orderId,
            Long originProductNo,
            Long itemNo,
            String productName,
            String productOption,
            DeliveryInfo deliveryInfo,
            String productOrderStatus,
            String placeOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity,
            LocalDateTime paymentDate,
            LocalDateTime shippingDueDate,
            String expectedDeliveryMethod,
            String deliveryCompany,
            String trackingNumber,
            Long unitPrice,
            Long paymentAmount,
            Long paymentCommission,
            Long saleCommission,
            Long channelCommission,
            Long expectedSettlementAmount
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
}
