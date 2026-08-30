import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Button, Table } from "react-bootstrap";
import { fetchFailedNotifications, retryNotification } from "./api";
import { ApiError } from "@/shared/api";
import { formatDateTime, NOTIFICATION_EVENT_LABEL } from "@/shared/lib";
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
      toast.show("알림을 다시 보내도록 요청했습니다. 기존 발송 건을 이어서 처리해 중복 발송을 막습니다.");
      queryClient.invalidateQueries({ queryKey });
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!data?.length) return <EmptyState message="다시 보낼 알림이 없습니다." />;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>알림 번호</th>
          <th>수신자</th>
          <th>알림 종류</th>
          <th>관련 항목</th>
          <th className="text-end">발송 시도</th>
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
            <td>{NOTIFICATION_EVENT_LABEL[notification.eventType] ?? "알림 유형 확인 필요"}</td>
            <td>{aggregateLabel(notification)}</td>
            <td className="text-end">{notification.attemptCount}</td>
            <td className="small">
              {notificationFailureLabel(notification.lastError)}
              {notification.lastError && (
                <details className="mt-1">
                  <summary>기술 상세</summary>
                  <pre className="mb-0 mt-1 text-wrap">{notification.lastError}</pre>
                </details>
              )}
            </td>
            <td className="small">{formatDateTime(notification.createdAt)}</td>
            <td>
              <Button
                size="sm"
                variant="outline-warning"
                disabled={pendingId === notification.outboxId}
                onClick={() => retry.mutate(notification.outboxId)}
              >
                {pendingId === notification.outboxId ? "요청 중..." : "다시 발송 요청"}
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
  const labels: Record<string, string> = {
    ORDER: "주문",
    BOOKING: "예약",
    PASS: "8회권",
    INQUIRY: "1:1 문의",
    PRODUCT_QNA: "상품 문의",
    REVIEW: "후기",
  };
  return `${labels[notification.aggregateType] ?? "관련 항목"} #${notification.aggregateId}`;
}

function notificationFailureLabel(reason: string | null | undefined): string {
  if (!reason) return "발송이 완료되지 않음";
  if (reason.includes("PERMANENT_DELIVERY_FAILURE")) return "수신 정보 문제로 발송할 수 없음";
  if (reason.includes("DELIVERY_RESULT_UNKNOWN")) return "발송 결과를 확인하지 못함";
  if (reason.includes("AUDIT_LOG_PERSISTENCE_FAILED")) return "발송 기록 저장을 확인하지 못함";
  return "원인을 확인하지 못함";
}
