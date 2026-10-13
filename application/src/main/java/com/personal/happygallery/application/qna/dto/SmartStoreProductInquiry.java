package com.personal.happygallery.application.qna.dto;

import java.time.LocalDateTime;

/** 스마트스토어 상품 문의다. */
public record SmartStoreProductInquiry(
        long questionId,
        long channelProductId,
        String productName,
        String maskedWriterId,
        String question,
        String answer,
        boolean answered,
        LocalDateTime createdAt
) {}
