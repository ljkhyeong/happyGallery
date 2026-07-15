package com.personal.happygallery.application.order.port.out;

/** 픽업 마감 알림 대상 주문의 수신자 조회 결과. */
public record PickupReminderTarget(Long orderId, Long userId, Long guestId) {}
