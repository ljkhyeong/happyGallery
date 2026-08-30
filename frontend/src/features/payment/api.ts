import {
  abandonPayment as requestPaymentAbandonment,
  confirmPayment as requestPaymentConfirmation,
  preparePayment as requestPaymentPreparation,
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
  return requestPaymentPreparation({ context, payload });
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
  return getPaymentStatus(encodeURIComponent(orderId), {
    headers: statusToken ? { "X-Payment-Status-Token": statusToken } : undefined,
  });
}

export function abandonPayment(orderId: string, statusToken: string | null): Promise<void> {
  return requestPaymentAbandonment(orderId, {
    headers: statusToken ? { "X-Payment-Status-Token": statusToken } : undefined,
  });
}

export function fetchPassPaymentPolicy(): Promise<PassPaymentPolicyResponse> {
  return getPassPaymentPolicy();
}
