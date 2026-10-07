package com.personal.happygallery.support;

import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderItemRef;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.order.FulfillmentType;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.util.List;

/**
 * 결제 준비 payload의 생략 값을 채우는 테스트 팩토리.
 *
 * <p>생략한 값은 장바구니 미사용, 픽업, 주문제작 동의·약관 동의·장바구니 버전·쿠폰·선택 항목 없음, 적립금 0원,
 * 예약 인원 1명이다.
 */
public final class PaymentPayloads {

    private PaymentPayloads() {}

    /** 옵션·직접입력 없는 주문 상품. */
    public static OrderItemRef item(Long productId, int qty) {
        return new OrderItemRef(productId, null, List.of(), qty);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items) {
        return order(userId, phone, verificationCode, name, items, false);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items,
            boolean cartCheckout) {
        return order(userId, phone, verificationCode, name, items, cartCheckout, FulfillmentType.PICKUP, null);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items,
            boolean cartCheckout, FulfillmentType fulfillmentType, ShippingAddress shippingAddress) {
        return order(userId, phone, verificationCode, name, items, cartCheckout,
                fulfillmentType, shippingAddress, null, false);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items,
            boolean cartCheckout, FulfillmentType fulfillmentType, ShippingAddress shippingAddress,
            String madeToOrderConsentVersion, boolean madeToOrderConsent) {
        return order(userId, phone, verificationCode, name, items, cartCheckout,
                fulfillmentType, shippingAddress, madeToOrderConsentVersion, madeToOrderConsent, null);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items,
            boolean cartCheckout, FulfillmentType fulfillmentType, ShippingAddress shippingAddress,
            String madeToOrderConsentVersion, boolean madeToOrderConsent, PolicyAcceptance policyAcceptance) {
        return order(userId, phone, verificationCode, name, items, cartCheckout,
                fulfillmentType, shippingAddress, madeToOrderConsentVersion, madeToOrderConsent,
                policyAcceptance, null);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items,
            boolean cartCheckout, FulfillmentType fulfillmentType, ShippingAddress shippingAddress,
            String madeToOrderConsentVersion, boolean madeToOrderConsent, PolicyAcceptance policyAcceptance,
            String expectedCartVersion) {
        return order(userId, phone, verificationCode, name, items, cartCheckout,
                fulfillmentType, shippingAddress, madeToOrderConsentVersion, madeToOrderConsent,
                policyAcceptance, expectedCartVersion, null, 0L);
    }

    public static OrderPayload order(
            Long userId, String phone, String verificationCode, String name, List<OrderItemRef> items,
            boolean cartCheckout, FulfillmentType fulfillmentType, ShippingAddress shippingAddress,
            String madeToOrderConsentVersion, boolean madeToOrderConsent, PolicyAcceptance policyAcceptance,
            String expectedCartVersion, Long issuedCouponId, long rewardAmount) {
        return new OrderPayload(userId, phone, verificationCode, name, items, cartCheckout,
                fulfillmentType, shippingAddress, madeToOrderConsentVersion, madeToOrderConsent,
                policyAcceptance, expectedCartVersion, issuedCouponId, rewardAmount, null);
    }

    public static BookingPayload booking(
            Long userId, String phone, String verificationCode, String name,
            Long slotId, Long passId, DepositPaymentMethod paymentMethod) {
        return booking(userId, phone, verificationCode, name, slotId, passId, paymentMethod, null);
    }

    public static BookingPayload booking(
            Long userId, String phone, String verificationCode, String name,
            Long slotId, Long passId, DepositPaymentMethod paymentMethod, PolicyAcceptance policyAcceptance) {
        return new BookingPayload(
                userId, phone, verificationCode, name, slotId, passId, paymentMethod, 1, policyAcceptance);
    }
}
