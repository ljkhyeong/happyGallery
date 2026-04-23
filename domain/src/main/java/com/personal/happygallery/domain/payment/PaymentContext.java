package com.personal.happygallery.domain.payment;

/** PG 결제 확정 대상 도메인. prepare 단계에서 어느 도메인으로 귀결될지 고정한다. */
public enum PaymentContext {
    ORDER,
    BOOKING,
    PASS
}
