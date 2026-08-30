package com.personal.happygallery.support;

import com.personal.happygallery.adapter.in.web.payment.dto.BookingPaymentPayloadRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.OrderPaymentPayloadRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.OrderPaymentPayloadRequest.OrderItemRefRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.OrderPaymentPayloadRequest.ShippingAddressRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.PassPaymentPayloadRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.PaymentPayloadRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.PreparePaymentRequest;
import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.BookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.OrderPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.order.ShippingAddress;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.List;

/** application 결제 입력을 실제 web request DTO로 만드는 통합 테스트 전용 mapper. */
public final class PaymentRequestFixtures {

    private PaymentRequestFixtures() {}

    public static PreparePaymentRequest prepareRequest(
            PaymentContext context,
            PaymentPayload payload
    ) {
        return new PreparePaymentRequest(context, payloadRequest(payload));
    }

    private static PaymentPayloadRequest payloadRequest(PaymentPayload payload) {
        return switch (payload) {
            case OrderPayload order -> new OrderPaymentPayloadRequest(
                    "ORDER",
                    order.userId(),
                    order.phone(),
                    order.verificationCode(),
                    order.name(),
                    orderItems(order.items()),
                    order.cartCheckout(),
                    order.fulfillmentType(),
                    shippingAddress(order.shippingAddress()),
                    order.madeToOrderConsentVersion(),
                    order.madeToOrderConsent(),
                    policyAcceptance(order.policyAcceptance()),
                    order.expectedCartVersion());
            case BookingPayload booking -> new BookingPaymentPayloadRequest(
                    "BOOKING",
                    booking.userId(),
                    booking.phone(),
                    booking.verificationCode(),
                    booking.name(),
                    booking.slotId(),
                    booking.passId(),
                    booking.paymentMethod(),
                    booking.participantCount(),
                    policyAcceptance(booking.policyAcceptance()));
            case PassPayload pass -> new PassPaymentPayloadRequest("PASS", pass.userId());
        };
    }

    private static List<OrderItemRefRequest> orderItems(
            List<PaymentPayload.OrderItemRef> items
    ) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(item -> new OrderItemRefRequest(item.productId(), item.qty()))
                .toList();
    }

    private static ShippingAddressRequest shippingAddress(ShippingAddress address) {
        if (address == null) {
            return null;
        }
        return new ShippingAddressRequest(
                address.recipientName(),
                address.phone(),
                address.postalCode(),
                address.addressLine1(),
                address.addressLine2());
    }

    private static PolicyAcceptanceRequest policyAcceptance(PolicyAcceptance acceptance) {
        if (acceptance == null) {
            return null;
        }
        return new PolicyAcceptanceRequest(
                acceptance.termsVersion(),
                acceptance.termsAccepted(),
                acceptance.privacyVersion(),
                acceptance.privacyAccepted());
    }
}
