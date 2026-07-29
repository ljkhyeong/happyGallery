import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Table } from "react-bootstrap";
import { fetchFailedNotifications, retryNotification } from "./api";
import { ApiError } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import type { FailedNotificationResponse } from "@/shared/types";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function FailedNotificationSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const queryKey = ["admin", "notifications", "failed"];
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchFailedNotifications(adminKey),
    refetchInterval: 10_000,
  });

  const retry = useAdminMutation(onAuthError, {
    mutationFn: (outboxId: number) => retryNotification(adminKey, outboxId),
    onMutate: (outboxId) => setPendingId(outboxId),
    onSuccess: () => {
      toast.show("알림을 같은 멱등키로 재처리 대기열에 넣었습니다.");
      queryClient.invalidateQueries({ queryKey });
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!data?.length) return <EmptyState message="재처리가 필요한 알림이 없습니다." />;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>ID</th>
          <th>수신자</th>
          <th>이벤트</th>
          <th>대상</th>
          <th className="text-end">시도</th>
          <th>실패 사유</th>
          <th>발생일</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        {data.map((notification) => (
          <tr key={notification.outboxId}>
            <td>{notification.outboxId}</td>
            <td>{recipientLabel(notification)}</td>
            <td>{notification.eventType}</td>
            <td>{aggregateLabel(notification)}</td>
            <td className="text-end">{notification.attemptCount}</td>
            <td className="small">{notification.lastError ?? "-"}</td>
            <td className="small">{formatDateTime(notification.createdAt)}</td>
            <td>
              <Button
                size="sm"
                variant="outline-warning"
                disabled={pendingId === notification.outboxId}
                onClick={() => retry.mutate(notification.outboxId)}
              >
                {pendingId === notification.outboxId ? "처리 중..." : "재처리"}
              </Button>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

function recipientLabel(notification: FailedNotificationResponse): string {
  return `${notification.recipientType === "USER" ? "회원" : "비회원"} ${notification.recipientId}`;
}

function aggregateLabel(notification: FailedNotificationResponse): string {
  if (!notification.aggregateType || notification.aggregateId == null) return "-";
  return `${notification.aggregateType} ${notification.aggregateId}`;
}
