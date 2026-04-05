import { useState, useCallback, useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Table, Button } from "react-bootstrap";
import { fetchFailedRefunds, retryRefund } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { formatKRW, formatDateTime } from "@/shared/lib";

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
      toast.show("환불 재시도 완료");
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
  if (!refunds?.length) return <EmptyState message="실패한 환불이 없습니다." />;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>환불 ID</th>
          <th>주문 ID</th>
          <th className="text-end">금액</th>
          <th>사유</th>
          <th>발생일</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {refunds.map((r) => (
          <tr key={r.refundId}>
            <td>{r.refundId}</td>
            <td>{r.orderId ?? r.bookingId ?? "-"}</td>
            <td className="text-end">{formatKRW(r.amount)}</td>
            <td className="small">{r.failReason}</td>
            <td className="small">{formatDateTime(r.createdAt)}</td>
            <td>
              <Button size="sm" variant="outline-warning"
                disabled={pendingId === r.refundId}
                onClick={() => handleRetry(r.refundId)}>
                {pendingId === r.refundId ? "..." : "재시도"}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
