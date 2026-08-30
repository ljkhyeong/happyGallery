package com.personal.happygallery.application.order.port.out;

import java.time.LocalDate;
import java.util.List;

public interface SmartStoreSettlementProvider {

    boolean isEnabled();

    List<SettlementItem> findByPayDate(LocalDate payDate);

    List<DailySettlement> findDailySettlements(LocalDate from, LocalDate to);

    List<CommissionDetail> findCommissionDetails(LocalDate from, LocalDate to);

    List<DailyVat> findDailyVat(LocalDate from, LocalDate to);

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

    record DailySettlement(
            LocalDate settleBasisStartDate,
            LocalDate settleBasisEndDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            long settleAmount,
            long paySettleAmount,
            long commissionSettleAmount,
            long benefitSettleAmount,
            long deductionRestoreSettleAmount,
            long payHoldbackAmount,
            long minusChargeAmount,
            long differenceSettleAmount,
            long returnCareSettleAmount,
            long normalSettleAmount,
            long quickSettleAmount,
            long preferentialCommissionAmount,
            long settlementLimitAmount,
            String settleMethodType,
            String merchantId,
            String merchantName
    ) {}

    record CommissionDetail(
            String orderNo,
            String productOrderId,
            String productOrderType,
            String productId,
            String productName,
            String merchantId,
            String merchantName,
            String settleType,
            LocalDate settleBasisDate,
            LocalDate settleExpectDate,
            LocalDate settleCompleteDate,
            LocalDate taxReturnDate,
            long commissionBasisAmount,
            String commissionType,
            String payMeansType,
            long commissionAmount,
            Long maximumSellingInterlockCommissionAmount
    ) {}

    record DailyVat(
            LocalDate settleBasisDate,
            long totalSalesAmount,
            long taxationSalesAmount,
            long taxExemptionSalesAmount,
            long creditCardAmount,
            long cashInComeDeductionAmount,
            long cashOutGoingEvidenceAmount,
            long cashExclusionIssuanceAmount,
            long otherAmount,
            String merchantId,
            String merchantName
    ) {}
}
