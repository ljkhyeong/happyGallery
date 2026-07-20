import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Table } from "react-bootstrap";
import { fetchPaymentReconciliations, reconcilePayment } from "./api";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { PaymentReconciliationRequiredResponse } from "@/shared/types";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const queryKey = ["admin", "payment-attempts", "reconciliation-required"];

export function PaymentReconciliationSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchPaymentReconciliations(adminKey),
    refetchInterval: 10_000,
  });

  const reconcile = useAdminMutation(onAuthError, {
    mutationFn: (attemptId: number) => reconcilePayment(adminKey, attemptId),
    onMutate: (attemptId) => setPendingId(attemptId),
    onSuccess: (result) => {
      const pending = result.status === "RECONCILIATION_REQUIRED";
      toast.show(result.message, pending ? "warning" : "success");
      queryClient.invalidateQueries({ queryKey });
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!data?.length) return <EmptyState message="대사가 필요한 결제가 없습니다." />;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>ID</th>
          <th>결제 대상</th>
          <th className="text-end">금액</th>
          <th>사유</th>
          <th>발생일</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {data.map((attempt) => (
          <tr key={attempt.attemptId}>
            <td>{attempt.attemptId}</td>
            <td>{contextLabel(attempt.context)}</td>
            <td className="text-end">{formatKRW(attempt.amount)}</td>
            <td className="small">{attempt.reason ?? "-"}</td>
            <td className="small">{attempt.createdAt ? formatDateTime(attempt.createdAt) : "-"}</td>
            <td>
              <Button
                size="sm"
                variant="outline-warning"
                disabled={pendingId === attempt.attemptId}
                onClick={() => reconcile.mutate(attempt.attemptId)}
              >
                {pendingId === attempt.attemptId ? "..." : "PG 조회"}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

function contextLabel(context: PaymentReconciliationRequiredResponse["context"]): string {
  if (context === "ORDER") return "주문";
  if (context === "BOOKING") return "예약";
  if (context === "PASS") return "8회권";
  return "-";
}
