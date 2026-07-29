import {
  confirmPayment as requestPaymentConfirmation,
  preparePayment as requestPaymentPreparation,
  type PreparePaymentRequest,
} from "@/generated/api/payment";
import {
  getPassPaymentPolicy,
  getPaymentStatus,
} from "@/generated/api/paymentQuery";
import type {
  ConfirmPaymentResponse,
  PaymentContext,
  PaymentPayload,
  PaymentStatusResponse,
  PassPaymentPolicyResponse,
  PreparePaymentResponse,
} from "./types";

export function preparePayment(
  context: PaymentContext,
  payload: PaymentPayload,
): Promise<PreparePaymentResponse> {
  return requestPaymentPreparation(toGeneratedPrepareRequest(context, payload));
}

export function confirmPayment(body: {
  paymentKey: string | null;
  orderId: string;
  amount: number;
}, statusToken: string | null = null): Promise<ConfirmPaymentResponse> {
  return requestPaymentConfirmation({
    orderId: body.orderId,
    amount: body.amount,
    ...(body.paymentKey === null ? {} : { paymentKey: body.paymentKey }),
  }, {
    headers: statusToken ? { "X-Payment-Status-Token": statusToken } : undefined,
  });
}

export function fetchPaymentStatus(
  orderId: string,
  statusToken: string | null,
): Promise<PaymentStatusResponse> {
  return getPaymentStatus(orderId, {
    headers: statusToken ? { "X-Payment-Status-Token": statusToken } : undefined,
  });
}

export function fetchPassPaymentPolicy(): Promise<PassPaymentPolicyResponse> {
  return getPassPaymentPolicy();
}

function toGeneratedPrepareRequest(
  context: PaymentContext,
  payload: PaymentPayload,
): PreparePaymentRequest {
  switch (payload.type) {
    case "ORDER":
      return {
        context,
        payload: {
          type: payload.type,
          cartCheckout: payload.cartCheckout,
          items: payload.items,
          madeToOrderConsent: payload.madeToOrderConsent,
          fulfillmentType: payload.fulfillmentType,
          ...(payload.userId == null ? {} : { userId: payload.userId }),
          ...(payload.phone == null ? {} : { phone: payload.phone }),
          ...(payload.verificationCode == null
            ? {}
            : { verificationCode: payload.verificationCode }),
          ...(payload.name == null ? {} : { name: payload.name }),
          ...(payload.madeToOrderConsentVersion == null
            ? {}
            : { madeToOrderConsentVersion: payload.madeToOrderConsentVersion }),
          ...(payload.policyAcceptance == null
            ? {}
            : { policyAcceptance: payload.policyAcceptance }),
          ...(payload.shippingAddress == null
            ? {}
            : {
                shippingAddress: {
                  recipientName: payload.shippingAddress.recipientName,
                  phone: payload.shippingAddress.phone,
                  postalCode: payload.shippingAddress.postalCode,
                  addressLine1: payload.shippingAddress.addressLine1,
                  ...(payload.shippingAddress.addressLine2 == null
                    ? {}
                    : { addressLine2: payload.shippingAddress.addressLine2 }),
                },
              }),
        },
      };
    case "BOOKING":
      return {
        context,
        payload: {
          type: payload.type,
          slotId: payload.slotId,
          participantCount: payload.participantCount,
          ...(payload.userId == null ? {} : { userId: payload.userId }),
          ...(payload.phone == null ? {} : { phone: payload.phone }),
          ...(payload.verificationCode == null
            ? {}
            : { verificationCode: payload.verificationCode }),
          ...(payload.name == null ? {} : { name: payload.name }),
          ...(payload.passId == null ? {} : { passId: payload.passId }),
          ...(payload.paymentMethod == null
            ? {}
            : { paymentMethod: payload.paymentMethod }),
          ...(payload.policyAcceptance == null
            ? {}
            : { policyAcceptance: payload.policyAcceptance }),
        },
      };
    case "PASS":
      return {
        context,
        payload: {
          type: payload.type,
          userId: payload.userId,
        },
      };
  }
}
