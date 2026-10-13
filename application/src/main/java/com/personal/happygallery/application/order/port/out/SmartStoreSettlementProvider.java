package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.application.order.dto.SmartStoreCommissionDetail;
import com.personal.happygallery.application.order.dto.SmartStoreDailySettlement;
import com.personal.happygallery.application.order.dto.SmartStoreDailyVat;
import java.time.LocalDate;
import java.util.List;

public interface SmartStoreSettlementProvider {

    boolean isEnabled();

    List<SettlementItem> findByPayDate(LocalDate payDate);

    List<SmartStoreDailySettlement> findDailySettlements(LocalDate from, LocalDate to);

    List<SmartStoreCommissionDetail> findCommissionDetails(LocalDate from, LocalDate to);

    List<SmartStoreDailyVat> findDailyVat(LocalDate from, LocalDate to);

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
