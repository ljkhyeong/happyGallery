import { useState, useCallback, useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Table, Button } from "react-bootstrap";
import { fetchFailedRefunds, retryRefund } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { formatKRW, formatDateTime } from "@/shared/lib";
import type { FailedRefundResponse } from "@/shared/types";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function FailedRefundSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pendingId, setPendingId] = useState<number | null>(null);

  const { data: refunds, isLoading, error } = useQuery({
    queryKey: ["admin", "refunds", "failed"],
    queryFn: () => fetchFailedRefunds(adminKey),
  });

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onAuthError();
    }
  }, [error, onAuthError]);

  const retry = useAdminMutation(onAuthError, {
    mutationFn: (refundId: number) => retryRefund(adminKey, refundId),
    onMutate: (id) => setPendingId(id),
    onSuccess: () => {
      toast.show("환불 재처리 요청 완료");
      queryClient.invalidateQueries({ queryKey: ["admin", "refunds", "failed"] });
    },
    onSettled: () => setPendingId(null),
  });

  const handleRetry = useCallback((id: number) => retry.mutate(id), [retry]);

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!refunds?.length) return <EmptyState message="확인이 필요한 환불이 없습니다." />;

  return (
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
                onClick={() => handleRetry(r.refundId)}>
                {pendingId === r.refundId ? "..." : "재처리"}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

function refundStatusLabel(status: FailedRefundResponse["status"]): string {
  if (status === "RECONCILIATION_REQUIRED") return "상태 확인 필요";
  if (status === "RETRYABLE") return "재시도 대기";
  return "실패 확정";
}

function refundTarget(refund: FailedRefundResponse): string {
  if (refund.orderId != null) return `주문 ${refund.orderId}`;
  if (refund.bookingId != null) return `예약 ${refund.bookingId}`;
  if (refund.passPurchaseId != null) return `8회권 ${refund.passPurchaseId}`;
  if (refund.paymentAttemptId != null) return `결제 시도 ${refund.paymentAttemptId}`;
  return "-";
}
