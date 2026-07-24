import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Table, Button } from "react-bootstrap";
import { fetchFailedRefunds, retryRefund } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatKRW, formatDateTime } from "@/shared/lib";
import type { AdminRefundStatus, FailedRefundResponse } from "@/shared/types";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function FailedRefundSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [cursor, setCursor] = useState<string | undefined>();
  const [cursorHistory, setCursorHistory] = useState<(string | undefined)[]>([]);

  const { data: page, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "refunds", "failed", cursor],
    queryFn: () => fetchFailedRefunds(adminKey, cursor),
    refetchInterval: 5_000,
  });
  const refunds = page?.content;

  const retry = useAdminMutation(onAuthError, {
    mutationFn: (refundId: number) => retryRefund(adminKey, refundId),
    onMutate: (id) => setPendingId(id),
    onSuccess: (result) => {
      showRetryResult(toast.show, result.status);
      queryClient.invalidateQueries({ queryKey: ["admin", "refunds", "failed"] });
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error instanceof ApiError && error.status === 401) return null;
  if (error && cursorHistory.length === 0) return <ErrorAlert error={error} />;
  if (!refunds?.length && cursorHistory.length === 0) {
    return <EmptyState message="확인이 필요한 환불이 없습니다." />;
  }

  function showNextPage() {
    if (!page?.nextCursor) return;
    setCursorHistory((history) => [...history, cursor]);
    setCursor(page.nextCursor);
  }

  function showPreviousPage() {
    setCursor(cursorHistory[cursorHistory.length - 1]);
    setCursorHistory(cursorHistory.slice(0, -1));
  }

  return (
    <>
      {error && <ErrorAlert error={error} />}
      {refunds && refunds.length > 0 ? (
        <Table responsive hover size="sm">
          <thead>
            <tr>
              <th>환불 ID</th>
              <th>대상</th>
              <th className="text-end">금액</th>
              <th>상태</th>
              <th className="text-end">시도</th>
              <th>사유</th>
              <th>발생일</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {refunds.map((r) => (
              <tr key={r.refundId}>
                <td>{r.refundId}</td>
                <td>{refundTarget(r)}</td>
                <td className="text-end">{formatKRW(r.amount)}</td>
                <td>{refundStatusLabel(r.status)}</td>
                <td className="text-end">{r.attemptCount}</td>
                <td className="small">{r.failReason}</td>
                <td className="small">{formatDateTime(r.createdAt)}</td>
                <td>
                  <Button size="sm" variant="outline-warning"
                    disabled={pendingId === r.refundId}
                    onClick={() => retry.mutate(r.refundId)}>
                    {pendingId === r.refundId ? "..." : "재처리"}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      ) : (
        <EmptyState message="이 페이지에 확인이 필요한 환불이 없습니다." />
      )}
      {(cursorHistory.length > 0 || page?.hasMore) && (
        <div className="d-flex justify-content-center gap-2 mb-3">
          <Button
            size="sm"
            variant="outline-secondary"
            disabled={cursorHistory.length === 0 || isLoading}
            onClick={showPreviousPage}
          >
            이전
          </Button>
          <Button
            size="sm"
            variant="outline-primary"
            disabled={!page?.hasMore || isLoading}
            onClick={showNextPage}
          >
            다음
          </Button>
        </div>
      )}
    </>
  );
}

function showRetryResult(
  show: ReturnType<typeof useToast>["show"],
  status: AdminRefundStatus["status"],
) {
  if (status === "SUCCEEDED") {
    show("환불 재처리가 완료되었습니다.");
  } else if (status === "FAILED") {
    show("환불 재처리가 실패했습니다. 실패 사유를 확인해 주세요.", "danger");
  } else {
    show("환불 재처리 후 상태를 확인 중입니다.", "warning");
  }
}

function refundStatusLabel(status: FailedRefundResponse["status"]): string {
  if (status === "RECONCILIATION_REQUIRED") return "상태 확인 필요";
  if (status === "RETRYABLE") return "재시도 대기";
  return "실패 확정";
}

function refundTarget(refund: FailedRefundResponse): string {
  if (refund.orderClaimId != null) {
    return refund.orderId != null
      ? `주문 ${refund.orderId} · 클레임 ${refund.orderClaimId}`
      : `클레임 ${refund.orderClaimId}`;
  }
  if (refund.orderId != null) return `주문 ${refund.orderId}`;
  if (refund.bookingId != null) return `예약 ${refund.bookingId}`;
  if (refund.passPurchaseId != null) return `8회권 ${refund.passPurchaseId}`;
  if (refund.paymentAttemptId != null) return `결제 시도 ${refund.paymentAttemptId}`;
  return "-";
}
