import { adminHeaders as h, api } from "@/shared/api";
import type {
  PaymentReconciliationRequiredResponse,
  PaymentReconciliationResultResponse,
} from "@/shared/types";

export function fetchPaymentReconciliations(
  adminKey: string,
): Promise<PaymentReconciliationRequiredResponse[]> {
  return api("/admin/payment-attempts/reconciliation-required", { headers: h(adminKey) });
}

export function reconcilePayment(
  adminKey: string,
  attemptId: number,
): Promise<PaymentReconciliationResultResponse> {
  return api(`/admin/payment-attempts/${attemptId}/reconcile`, {
    method: "POST",
    headers: h(adminKey),
  });
}
