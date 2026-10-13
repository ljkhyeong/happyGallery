package com.personal.happygallery.application.order.dto;

import java.time.LocalDate;

/** 스마트스토어 수수료 상세 내역이다. */
public record SmartStoreCommissionDetail(
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
