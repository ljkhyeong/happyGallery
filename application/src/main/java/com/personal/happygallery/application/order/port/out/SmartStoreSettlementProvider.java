package com.personal.happygallery.application.order.port.out;

import java.time.LocalDate;
import java.util.List;

public interface SmartStoreSettlementProvider {

    boolean isEnabled();

    List<SettlementItem> findByPayDate(LocalDate payDate);

    record SettlementItem(
            String productOrderId,
            String orderId,
            String productOrderType,
            String settleType,
            String productName,
            long paySettleAmount,
            Long totalPayCommissionAmount,
            Long sellingInterlockCommissionAmount,
            long benefitSettleAmount,
            long settleExpectAmount,
            LocalDate settleBasisDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            LocalDate payDate
    ) {}
}
