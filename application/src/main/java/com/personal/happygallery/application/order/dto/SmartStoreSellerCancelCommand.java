package com.personal.happygallery.application.order.dto;

/** 판매자가 직접 접수하는 스마트스토어 취소 요청이다. */
public record SmartStoreSellerCancelCommand(
        String productOrderId,
        String reason,
        String detailedReason,
        Integer quantity
) {}
