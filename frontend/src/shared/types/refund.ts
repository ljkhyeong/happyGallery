export type RefundStatus =
  | "REQUESTED"
  | "PROCESSING"
  | "RETRYABLE"
  | "RECONCILIATION_REQUIRED"
  | "SUCCEEDED"
  | "FAILED";

export interface RefundProgress {
  amount: number;
  status: RefundStatus;
}

export interface AdminRefundStatus extends RefundProgress {
  refundId: number;
  attemptCount: number;
  failReason: string | null;
}
