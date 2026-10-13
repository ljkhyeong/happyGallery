package com.personal.happygallery.application.order.dto;

/** 스마트스토어 주문 배송정보다. 원장에는 암호문으로만 저장한다. */
public record SmartStoreDeliveryInfo(
        String recipientName,
        String phone,
        String postalCode,
        String addressLine1,
        String addressLine2,
        String shippingMemo
) {}
