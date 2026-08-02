import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Modal, Table } from "react-bootstrap";
import { fetchFailedRefunds, retryRefund } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
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
  const [retryTarget, setRetryTarget] = useState<FailedRefundResponse | null>(null);
  const {
    cursor,
    hasPreviousPage,
    showNextPage,
    showPreviousPage,
  } = useCursorHistory();

  const { data: page, isLoading, isFetching, error, refetch } = useAdminQuery(onAuthError, {
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
      setRetryTarget(null);
      queryClient.invalidateQueries({ queryKey: ["admin", "refunds", "failed"] });
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error instanceof ApiError && error.status === 401) return null;
  if (error && page === undefined && !hasPreviousPage) {
    return (
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching}
      />
    );
  }
  if (!error && !refunds?.length && !hasPreviousPage) {
    return <EmptyState message="확인이 필요한 환불이 없습니다." />;
  }

  return (
    <>
      {error && (
        <ErrorAlert
          error={error}
          onRetry={() => { void refetch(); }}
          retrying={isFetching}
        />
      )}
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
                    onClick={() => {
                      retry.reset();
                      setRetryTarget(r);
                    }}>
                    {pendingId === r.refundId ? "처리 중..." : "재처리"}
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      ) : !error && refunds !== undefined ? (
        <EmptyState message="이 페이지에 확인이 필요한 환불이 없습니다." />
      ) : null}
      {(hasPreviousPage || page?.hasMore) && (
        <div className="d-flex justify-content-center gap-2 mb-3">
          <Button
            size="sm"
            variant="outline-secondary"
            disabled={!hasPreviousPage || isLoading}
            onClick={showPreviousPage}
          >
            이전
          </Button>
          <Button
            size="sm"
            variant="outline-primary"
            disabled={!page?.hasMore || isLoading}
            onClick={() => showNextPage(page?.nextCursor)}
          >
            다음
          </Button>
        </div>
      )}
      <Modal
        show={retryTarget !== null}
        aria-labelledby="admin-failed-refund-retry-title"
        onHide={() => {
          if (pendingId === null) setRetryTarget(null);
        }}
        centered
      >
        <Modal.Header closeButton={pendingId === null}>
          <Modal.Title id="admin-failed-refund-retry-title" className="fs-6">
            환불 재처리 영향 확인
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={retry.error} />
          {retryTarget && (
            <>
              <dl className="row small mb-3">
                <dt className="col-5">대상</dt>
                <dd className="col-7">{refundTarget(retryTarget)}</dd>
                <dt className="col-5">환불 금액</dt>
                <dd className="col-7 fw-semibold">{formatKRW(retryTarget.amount)}</dd>
                <dt className="col-5">현재 상태</dt>
                <dd className="col-7">{refundStatusLabel(retryTarget.status)}</dd>
                <dt className="col-5">기존 시도</dt>
                <dd className="col-7">{retryTarget.attemptCount}회</dd>
              </dl>
              <Alert variant="warning" className="small mb-0">
                {retryTarget.status === "RECONCILIATION_REQUIRED"
                  ? "결제사 반영 상태를 먼저 조회합니다. 결과가 불명확하면 중복 취소하지 않고 확인 필요 상태를 유지합니다."
                  : "새 환불을 만들지 않고 기존 환불 요청의 동일한 멱등키로 결제사 취소를 다시 요청합니다. 결과가 불명확하면 확인 필요 상태로 전환합니다."}
              </Alert>
            </>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            disabled={pendingId !== null}
            onClick={() => setRetryTarget(null)}
          >
            닫기
          </Button>
          <Button
            variant="warning"
            disabled={!retryTarget || pendingId !== null}
            onClick={() => retryTarget && retry.mutate(retryTarget.refundId)}
          >
            {pendingId !== null ? "재처리 중..." : "확인하고 재처리"}
          </Button>
        </Modal.Footer>
      </Modal>
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
