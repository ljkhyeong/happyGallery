package com.personal.happygallery.application.order.dto;

/** 판매자가 직접 접수하는 스마트스토어 반품 요청이다. */
public record SmartStoreSellerReturnCommand(
        String productOrderId,
        String returnReason,
        String collectDeliveryMethod,
        String collectDeliveryCompany,
        String collectTrackingNumber,
        Integer returnQuantity
) {}
