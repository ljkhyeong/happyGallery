import {
  listRequired,
  listPaymentSettlementIssues,
  reconcile,
  type PaymentReconciliationRequiredResponse,
  type PaymentReconciliationResultResponse,
  type PaymentSettlementIssueResponse,
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

export function fetchPaymentSettlementIssues(
  adminKey: string,
): Promise<PaymentSettlementIssueResponse[]> {
  return listPaymentSettlementIssues({ headers: adminHeaders(adminKey) });
}
