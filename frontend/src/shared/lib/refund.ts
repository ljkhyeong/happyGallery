import type { RefundStatus } from "@/shared/types";

const FAST_POLL_INTERVAL_MS = 3_000;
const SLOW_POLL_INTERVAL_MS = 15_000;
const FAST_POLL_LIMIT = 20;

export function isRefundActivelyProcessing(status: RefundStatus | undefined): boolean {
  return status === "REQUESTED" || status === "PROCESSING";
}

export function customerRefundPollingInterval(
  status: RefundStatus | undefined,
  pollCount: number,
): number | false {
  if (status === "RETRYABLE" || status === "RECONCILIATION_REQUIRED") {
    return SLOW_POLL_INTERVAL_MS;
  }
  if (!isRefundActivelyProcessing(status)) {
    return false;
  }
  return pollCount < FAST_POLL_LIMIT ? FAST_POLL_INTERVAL_MS : SLOW_POLL_INTERVAL_MS;
}

export function adminRefundPollingInterval(pollCount: number): number {
  return pollCount < FAST_POLL_LIMIT ? FAST_POLL_INTERVAL_MS : SLOW_POLL_INTERVAL_MS;
}
