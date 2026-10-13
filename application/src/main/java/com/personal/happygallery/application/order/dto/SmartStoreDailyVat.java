package com.personal.happygallery.application.order.dto;

import java.time.LocalDate;

/** 스마트스토어 일별 부가세 신고 내역이다. */
public record SmartStoreDailyVat(
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
