package com.personal.happygallery.application.order.dto;

/** 스마트스토어 일괄 처리에서 실패한 상품 주문과 실패 코드·사유다. */
public record SmartStoreOperationFailure(String productOrderId, String code, String message) {}
