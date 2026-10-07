package com.personal.happygallery.application.order.dto;

/** 스마트스토어 교환 재배송 처리 요청이다. */
public record SmartStoreExchangeDispatchCommand(
        String productOrderId,
        String deliveryMethod,
        String deliveryCompanyCode,
        String trackingNumber
) {}
