import type { OrderClaimResponse } from "@/generated/api/orderClaim";
import { customerRefundPollingInterval } from "@/shared/lib";

export const ORDER_CLAIM_SLOW_POLL_INTERVAL_MS = 15_000;

export function orderClaimPollingInterval(
  claims: readonly OrderClaimResponse[] | undefined,
  pollCount: number,
): number | false {
  const refundStatuses = (claims ?? [])
    .filter((claim) => claim.status === "REFUND_REQUESTED")
    .map((claim) => claim.refundStatus);
  const status = refundStatuses.find(
    (candidate) => candidate === "REQUESTED" || candidate === "PROCESSING",
  ) ?? refundStatuses.find(
    (candidate) => candidate === "RETRYABLE"
      || candidate === "RECONCILIATION_REQUIRED",
  );

  const refundInterval = customerRefundPollingInterval(status ?? undefined, pollCount);
  if (refundInterval !== false) return refundInterval;

  return claims?.some((claim) => claim.status !== "REJECTED" && claim.status !== "COMPLETED")
    ? ORDER_CLAIM_SLOW_POLL_INTERVAL_MS
    : false;
}
