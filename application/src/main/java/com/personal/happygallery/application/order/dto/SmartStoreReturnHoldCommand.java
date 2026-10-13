package com.personal.happygallery.application.order.dto;

/** 스마트스토어 반품 보류 요청이다. */
public record SmartStoreReturnHoldCommand(
        String productOrderId,
        String holdbackClassType,
        String detailedReason,
        Long extraReturnFeeAmount
) {}
