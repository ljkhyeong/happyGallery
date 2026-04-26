import { api } from "@/shared/api";
import type {
  ConfirmPaymentResponse,
  PaymentContext,
  PaymentPayload,
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
}): Promise<ConfirmPaymentResponse> {
  return api<ConfirmPaymentResponse>("/payments/confirm", {
    method: "POST",
    body,
  });
}
