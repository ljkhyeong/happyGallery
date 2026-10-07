package com.personal.happygallery.application.order.dto;

/** 스마트스토어 교환 보류 요청이다. */
public record SmartStoreExchangeHoldCommand(
        String productOrderId,
        String holdbackClassType,
        String detailedReason,
        Long extraExchangeFeeAmount
) {}
