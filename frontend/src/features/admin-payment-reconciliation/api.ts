import {
  listRequired,
  listPaymentSettlementIssues,
  listSmartStoreSettlementIssues,
  reconcile,
  synchronizeSmartStoreSettlements,
  type PaymentReconciliationRequiredResponse,
  type PaymentReconciliationResultResponse,
  type PaymentSettlementIssueResponse,
  type SmartStoreSettlementIssueResponse,
  type SmartStoreSettlementSyncResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export function fetchPaymentReconciliations(
  adminKey: string,
): Promise<PaymentReconciliationRequiredResponse[]> {
  return listRequired({ headers: adminHeaders(adminKey) });
}

export function fetchSmartStoreSettlementIssues(
  adminKey: string,
): Promise<SmartStoreSettlementIssueResponse[]> {
  return listSmartStoreSettlementIssues({ headers: adminHeaders(adminKey) });
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

export function synchronizeSmartStoreSettlementRange(
  adminKey: string,
  from: string,
  to: string,
): Promise<SmartStoreSettlementSyncResponse> {
  return synchronizeSmartStoreSettlements(
    { from, to },
    { headers: adminHeaders(adminKey) },
  );
}
