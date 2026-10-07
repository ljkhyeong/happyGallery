package com.personal.happygallery.application.order.dto;

import java.time.LocalDate;

/** 스마트스토어 일별 정산 내역이다. */
public record SmartStoreDailySettlement(
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
