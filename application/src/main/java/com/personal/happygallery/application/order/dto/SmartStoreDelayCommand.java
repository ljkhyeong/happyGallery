package com.personal.happygallery.application.order.dto;

import java.time.LocalDateTime;

/** 스마트스토어 발송 지연 처리 요청이다. */
public record SmartStoreDelayCommand(
        String productOrderId,
        LocalDateTime dispatchDueDate,
        String reasonCode,
        String detailedReason
) {}
