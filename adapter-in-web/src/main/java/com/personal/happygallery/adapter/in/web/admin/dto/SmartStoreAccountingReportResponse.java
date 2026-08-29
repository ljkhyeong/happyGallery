package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase.AccountingReport;
import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase.CommissionDetail;
import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase.DailySettlement;
import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase.DailyVat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public record SmartStoreAccountingReportResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate from,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate to,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate vatAvailableThrough,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DailySettlementResponse> dailySettlements,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CommissionDetailResponse> commissionDetails,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<DailyVatResponse> dailyVat
) {
    public static SmartStoreAccountingReportResponse from(AccountingReport report) {
        return new SmartStoreAccountingReportResponse(
                report.from(), report.to(), report.vatAvailableThrough(),
                report.dailySettlements().stream().map(DailySettlementResponse::from).toList(),
                report.commissionDetails().stream().map(CommissionDetailResponse::from).toList(),
                report.dailyVat().stream().map(DailyVatResponse::from).toList());
    }

    public record DailySettlementResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate settleBasisStartDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate settleBasisEndDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate settleExpectDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleCompleteDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long settleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long paySettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long commissionSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long benefitSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long deductionRestoreSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long payHoldbackAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long minusChargeAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long differenceSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long returnCareSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long normalSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long quickSettleAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long preferentialCommissionAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long settlementLimitAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String settleMethodType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String merchantId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String merchantName
    ) {
        private static DailySettlementResponse from(DailySettlement settlement) {
            return new DailySettlementResponse(
                    settlement.settleBasisStartDate(), settlement.settleBasisEndDate(),
                    settlement.settleExpectDate(), settlement.settleCompleteDate(),
                    settlement.settleAmount(), settlement.paySettleAmount(),
                    settlement.commissionSettleAmount(), settlement.benefitSettleAmount(),
                    settlement.deductionRestoreSettleAmount(), settlement.payHoldbackAmount(),
                    settlement.minusChargeAmount(), settlement.differenceSettleAmount(),
                    settlement.returnCareSettleAmount(), settlement.normalSettleAmount(),
                    settlement.quickSettleAmount(), settlement.preferentialCommissionAmount(),
                    settlement.settlementLimitAmount(), settlement.settleMethodType(),
                    settlement.merchantId(), settlement.merchantName());
        }
    }

    public record CommissionDetailResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String orderNo,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String merchantId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String merchantName,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String settleType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleBasisDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleExpectDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleCompleteDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate taxReturnDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long commissionBasisAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String commissionType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String payMeansType,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long commissionAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long maximumSellingInterlockCommissionAmount
    ) {
        private static CommissionDetailResponse from(CommissionDetail detail) {
            return new CommissionDetailResponse(
                    detail.orderNo(), detail.productOrderId(), detail.productOrderType(),
                    detail.productId(), detail.productName(), detail.merchantId(),
                    detail.merchantName(), detail.settleType(), detail.settleBasisDate(),
                    detail.settleExpectDate(), detail.settleCompleteDate(), detail.taxReturnDate(),
                    detail.commissionBasisAmount(), detail.commissionType(), detail.payMeansType(),
                    detail.commissionAmount(), detail.maximumSellingInterlockCommissionAmount());
        }
    }

    public record DailyVatResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate settleBasisDate,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalSalesAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long taxationSalesAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long taxExemptionSalesAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long creditCardAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long cashInComeDeductionAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long cashOutGoingEvidenceAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long cashExclusionIssuanceAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long otherAmount,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String merchantId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String merchantName
    ) {
        private static DailyVatResponse from(DailyVat vat) {
            return new DailyVatResponse(
                    vat.settleBasisDate(), vat.totalSalesAmount(), vat.taxationSalesAmount(),
                    vat.taxExemptionSalesAmount(), vat.creditCardAmount(),
                    vat.cashInComeDeductionAmount(), vat.cashOutGoingEvidenceAmount(),
                    vat.cashExclusionIssuanceAmount(), vat.otherAmount(),
                    vat.merchantId(), vat.merchantName());
        }
    }
}
