import { api } from "@/shared/api";
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
  return api<PreparePaymentResponse>("/payments/prepare", {
    method: "POST",
    body: { context, payload },
  });
}

export function confirmPayment(body: {
  paymentKey: string | null;
  orderId: string;
  amount: number;
}, statusToken: string | null = null): Promise<ConfirmPaymentResponse> {
  return api<ConfirmPaymentResponse>("/payments/confirm", {
    method: "POST",
    headers: statusToken ? { "X-Payment-Status-Token": statusToken } : undefined,
    body,
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
