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
  BALANCE_SETTLEMENT: "결제된 잔금 정산",
  MANUAL_COMPENSATION: "수동 환불·보상",
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
        result.changed ? "예약 취소 후속 작업을 완료했습니다." : "이미 완료된 작업입니다.",
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
    return <EmptyState message="미처리 예약 취소 후속 작업이 없습니다." />;
  }

  return (
    <>
      <ErrorAlert error={completion.error} />
      <Table responsive hover size="sm">
        <thead>
          <tr>
            <th>예약</th>
            <th>작업</th>
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
                    반환할 예약금 {formatKRW(task.compensationAmount)}
                  </small>
                )}
                {task.type === "MANUAL_COMPENSATION" && task.compensationAmount === 0 && (
                  <small className="d-block mt-1">8회권 대체 보상 확인</small>
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
                  {pendingId === task.taskId ? "처리 중" : "완료"}
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
    </>
  );
}
