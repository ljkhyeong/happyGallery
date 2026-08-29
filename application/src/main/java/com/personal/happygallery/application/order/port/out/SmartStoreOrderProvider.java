package com.personal.happygallery.application.order.port.out;

import java.time.LocalDateTime;
import java.util.List;

public interface SmartStoreOrderProvider {

    boolean isEnabled();

    ChangePage fetchChanges(ChangeCursor cursor, LocalDateTime changedTo);

    List<ProductOrderDetail> fetchDetails(List<String> productOrderIds);

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
            String productOrderStatus,
            String claimType,
            String claimStatus,
            int initialQuantity,
            int remainQuantity,
            LocalDateTime paymentDate
    ) {}
}
