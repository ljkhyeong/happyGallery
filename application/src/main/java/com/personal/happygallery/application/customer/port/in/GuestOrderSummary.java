package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 비회원 기록 인수 미리보기와 기록 복구 목록이 함께 쓰는 비회원 주문 요약. */
public record GuestOrderSummary(Long orderId, OrderStatus status, long totalAmount, OffsetDateTime createdAt) {

    public static GuestOrderSummary from(Order order) {
        return new GuestOrderSummary(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
