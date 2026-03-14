package com.personal.happygallery.infra.payment;

/**
 * 환불 호출 컨텍스트를 ThreadLocal로 전달한다.
 * RefundExecutionService가 PG 호출 전에 설정하고, FakePaymentProvider가 읽는다.
 */
public final class RefundContext {

    private static final ThreadLocal<Long> CURRENT_ORDER_ID = new ThreadLocal<>();

    private RefundContext() {}

    public static void setOrderId(Long orderId) {
        CURRENT_ORDER_ID.set(orderId);
    }

    public static Long currentOrderId() {
        return CURRENT_ORDER_ID.get();
    }

    public static void clear() {
        CURRENT_ORDER_ID.remove();
    }
}
