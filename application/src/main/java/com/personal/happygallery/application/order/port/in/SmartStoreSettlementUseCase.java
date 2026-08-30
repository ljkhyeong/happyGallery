package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import java.util.List;
import java.time.LocalDate;

public interface SmartStoreSettlementUseCase {

    BatchResult synchronizeRecent();

    BatchResult synchronize(LocalDate from, LocalDate to);

    List<SmartStoreSettlementEntry> findIssues(int limit);

    AccountingReport accounting(LocalDate from, LocalDate to);

    record AccountingReport(
            LocalDate from,
            LocalDate to,
            LocalDate vatAvailableThrough,
            List<DailySettlement> dailySettlements,
            List<CommissionDetail> commissionDetails,
            List<DailyVat> dailyVat
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
