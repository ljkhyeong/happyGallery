package com.personal.happygallery.application.qna.dto;

import java.time.LocalDateTime;

/** 스마트스토어 고객 문의다. */
public record SmartStoreCustomerInquiry(
        long inquiryNo,
        Long answerContentId,
        String category,
        String title,
        String inquiryContent,
        String answerContent,
        boolean answered,
        String orderId,
        String channelProductId,
        String productOrderIds,
        String productName,
        String productOrderOption,
        String maskedCustomerId,
        String customerName,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {}
