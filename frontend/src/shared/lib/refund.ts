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

interface RefundPollingQuery<TData> {
  state: {
    data: TData | undefined;
    dataUpdateCount: number;
    fetchFailureCount: number;
  };
}

/**
 * 환불 진행 상태가 담긴 고객 조회의 refetchInterval. 응답에서 환불 상태를 고르는 함수만 받고,
 * 성공·실패 응답 횟수를 합쳐 빠른 확인 주기를 끝낼 시점을 정한다.
 */
export function customerRefundRefetchInterval<TData>(
  refundStatus: (data: TData) => RefundStatus | null | undefined,
) {
  return ({ state }: RefundPollingQuery<TData>): number | false => customerRefundPollingInterval(
    state.data === undefined ? undefined : refundStatus(state.data) ?? undefined,
    state.dataUpdateCount + state.fetchFailureCount,
  );
}

export function adminRefundPollingInterval(pollCount: number): number {
  return pollCount < FAST_POLL_LIMIT ? FAST_POLL_INTERVAL_MS : SLOW_POLL_INTERVAL_MS;
}
