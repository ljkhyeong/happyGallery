import { useCallback, useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/shared/api";
import { adminRefundPollingInterval, isRefundActivelyProcessing } from "@/shared/lib";
import { useToast } from "@/shared/ui";
import type { RefundStatus } from "@/shared/types";
import { fetchRefund } from "./api";

const POLL_ERROR_RETRY_DELAY_MS = 15_000;
const MAX_POLL_ERRORS = 3;

export function useAdminRefundPolling(adminKey: string, onAuthError: () => void) {
  const queryClient = useQueryClient();
  const { show } = useToast();
  const activeRefundIds = useRef(new Set<number>());

  const pollRefund = useCallback(async (refundId: number, label: string) => {
    let pollCount = 0;
    let errorCount = 0;

    while (activeRefundIds.current.has(refundId)) {
      try {
        const result = await queryClient.fetchQuery({
          queryKey: ["admin", "refund", refundId],
          queryFn: () => fetchRefund(adminKey, refundId),
          staleTime: 0,
        });
        if (!activeRefundIds.current.has(refundId)) return;

        if (!isRefundActivelyProcessing(result.status)) {
          activeRefundIds.current.delete(refundId);
          showRefundResult(show, label, result.status);
          void queryClient.invalidateQueries({ queryKey: ["admin", "refunds", "failed"] });
          return;
        }

        errorCount = 0;
        await wait(adminRefundPollingInterval(pollCount++));
      } catch (error) {
        if (!activeRefundIds.current.has(refundId)) return;
        if (error instanceof ApiError && error.status === 401) {
          activeRefundIds.current.delete(refundId);
          onAuthError();
          return;
        }
        if (++errorCount >= MAX_POLL_ERRORS) {
          activeRefundIds.current.delete(refundId);
          show(`${label} 환불 상태를 자동 확인하지 못했습니다.`, "warning");
          return;
        }
        await wait(POLL_ERROR_RETRY_DELAY_MS);
      }
    }
  }, [adminKey, onAuthError, queryClient, show]);

  useEffect(() => {
    const refundIds = activeRefundIds.current;
    return () => refundIds.clear();
  }, []);

  const trackRefund = useCallback((refundId: number, label: string) => {
    if (activeRefundIds.current.has(refundId)) return;
    activeRefundIds.current.add(refundId);
    void pollRefund(refundId, label);
  }, [pollRefund]);

  return { trackRefund };
}

function wait(delay: number): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, delay));
}

function showRefundResult(
  show: ReturnType<typeof useToast>["show"],
  label: string,
  status: RefundStatus,
) {
  if (status === "SUCCEEDED") {
    show(`${label} 환불이 완료되었습니다.`);
  } else if (status === "FAILED") {
    show(`${label} 환불이 완료되지 않았습니다. 확인 필요 목록을 확인해 주세요.`, "danger");
  } else {
    show(`${label} 환불 상태를 확인 중입니다. 확인 필요 목록에서 계속 추적합니다.`, "warning");
  }
}
