package com.personal.happygallery.application.order.dto;

import java.time.LocalDateTime;

/** 스마트스토어 발송 처리 요청이다. */
public record SmartStoreDispatchCommand(
        String productOrderId,
        String deliveryMethod,
        String deliveryCompanyCode,
        String trackingNumber,
        LocalDateTime dispatchDate
) {}
