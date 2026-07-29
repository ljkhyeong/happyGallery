import {
  listRequired,
  reconcile,
  type PaymentReconciliationRequiredResponse,
  type PaymentReconciliationResultResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export function fetchPaymentReconciliations(
  adminKey: string,
): Promise<PaymentReconciliationRequiredResponse[]> {
  return listRequired({ headers: adminHeaders(adminKey) });
}

export function reconcilePayment(
  adminKey: string,
  attemptId: number,
): Promise<PaymentReconciliationResultResponse> {
  return reconcile(attemptId, { headers: adminHeaders(adminKey) });
}
