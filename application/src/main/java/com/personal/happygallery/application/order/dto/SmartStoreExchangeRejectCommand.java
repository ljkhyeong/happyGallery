package com.personal.happygallery.application.order.dto;

/** 스마트스토어 교환 거부 요청이다. */
public record SmartStoreExchangeRejectCommand(String productOrderId, String reason) {}
