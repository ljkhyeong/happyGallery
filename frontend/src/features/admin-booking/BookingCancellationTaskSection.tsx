import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Table } from "react-bootstrap";
import { Check } from "lucide-react";
import { Link } from "react-router";
import {
  completeBookingCancellationTask,
  fetchBookingCancellationTasks,
  type BookingCancellationTaskType,
} from "./api";
import { ApiError, queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const TYPE_LABELS: Record<BookingCancellationTaskType, string> = {
  BALANCE_SETTLEMENT: "고객에게 받은 잔금 직접 반환",
  MANUAL_COMPENSATION: "직접 처리할 환불·보상",
};

export function BookingCancellationTaskSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const { data: tasks, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.bookingCancellationTasks,
    queryFn: () => fetchBookingCancellationTasks(adminKey),
    refetchInterval: 10_000,
  });

  const completion = useAdminMutation(onAuthError, {
    mutationFn: (taskId: number) => completeBookingCancellationTask(adminKey, taskId),
    onMutate: setPendingId,
    onSuccess: (result) => {
      toast.show(
        result.changed ? "직접 처리할 항목을 완료로 표시했습니다." : "이미 완료로 표시된 항목입니다.",
        result.changed ? "success" : "info",
      );
      queryClient.invalidateQueries({
        queryKey: queryKeys.admin.bookingCancellationTasks,
      });
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error instanceof ApiError && error.status === 401) return null;
  if (error) return <ErrorAlert error={error} />;
  if (!tasks?.length) {
    return <EmptyState message="예약 취소 후 직접 처리할 일이 없습니다." />;
  }

  return (
    <>
      <ErrorAlert error={completion.error} />
      <Table responsive hover size="sm">
        <thead>
          <tr>
            <th>예약</th>
            <th>해야 할 일</th>
            <th>취소 사유</th>
            <th>발생일</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {tasks.map((task) => (
            <tr key={task.taskId}>
              <td>
                <Link
                  className="fw-semibold"
                  to={`/admin?view=bookings&bookingId=${task.bookingId}`
                    + `&bookingDate=${task.startAt.slice(0, 10)}&bookingStatus=CANCELED`}
                >
                  {task.bookingNumber}
                </Link>
                <small className="d-block">{task.className}</small>
                <small className="d-block text-muted-soft">{formatDateTime(task.startAt)}</small>
              </td>
              <td>
                <Badge bg={task.type === "BALANCE_SETTLEMENT" ? "warning" : "danger"}
                  text={task.type === "BALANCE_SETTLEMENT" ? "dark" : undefined}>
                  {TYPE_LABELS[task.type]}
                </Badge>
                {task.type === "BALANCE_SETTLEMENT" && (
                  <small className="d-block mt-1">{formatKRW(task.balanceAmount)}</small>
                )}
                {task.type === "MANUAL_COMPENSATION" && task.compensationAmount > 0 && (
                  <small className="d-block mt-1">
                    고객에게 예약금 {formatKRW(task.compensationAmount)} 직접 반환
                  </small>
                )}
                {task.type === "MANUAL_COMPENSATION" && task.compensationAmount === 0 && (
                  <small className="d-block mt-1">만료된 8회권 보상 방법을 고객과 협의</small>
                )}
              </td>
              <td className="small">{task.reason}</td>
              <td className="small">{formatDateTime(task.createdAt)}</td>
              <td>
                <Button
                  size="sm"
                  variant="outline-success"
                  disabled={completion.isPending}
                  onClick={() => completion.mutate(task.taskId)}
                >
                  <Check size={14} aria-hidden="true" className="me-1" />
                  {pendingId === task.taskId ? "처리 중..." : "처리 완료로 표시"}
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  );
}
