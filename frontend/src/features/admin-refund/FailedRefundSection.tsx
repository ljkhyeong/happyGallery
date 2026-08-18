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
              <th>환불 번호</th>
              <th>대상</th>
              <th className="text-end">금액</th>
              <th>상태</th>
              <th className="text-end">처리 시도</th>
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
                <td className="small">
                  {refundFailureReason(r.status)}
                  <details className="mt-1">
                    <summary>기술 상세</summary>
                    <pre className="mb-0 mt-1 text-wrap">{r.failReason}</pre>
                  </details>
                </td>
                <td className="small">{formatDateTime(r.createdAt)}</td>
                <td>
                  <Button size="sm" variant="outline-warning"
                    disabled={pendingId === r.refundId}
                    onClick={() => {
                      retry.reset();
                      setRetryTarget(r);
                    }}>
                    {pendingId === r.refundId
                      ? "확인 중..."
                      : r.status === "RECONCILIATION_REQUIRED"
                        ? "환불 여부 확인"
                        : "환불 다시 요청"}
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
            환불 처리 다시 확인
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
                  ? "결제사에 이미 환불됐는지 먼저 확인합니다. 결과를 확인할 수 없으면 새 환불을 요청하지 않고 이 목록에 남깁니다."
                  : "새 환불 건을 만들지 않고 기존 환불 건으로 결제사에 다시 요청합니다. 결과를 확인할 수 없으면 결제사 확인 필요 상태로 남깁니다."}
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
            {pendingId !== null
              ? "확인 중..."
              : retryTarget?.status === "RECONCILIATION_REQUIRED"
                ? "결제사에서 상태 확인"
                : "기존 환불 다시 요청"}
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
    show("기존 환불 처리가 완료되었습니다.");
  } else if (status === "FAILED") {
    show("결제사에서 환불을 완료하지 못했습니다. 확인 필요 사유를 살펴봐 주세요.", "danger");
  } else {
    show("기존 환불 건의 처리 상태를 확인하고 있습니다.", "warning");
  }
}

function refundStatusLabel(status: FailedRefundResponse["status"]): string {
  if (status === "RECONCILIATION_REQUIRED") return "결제사 확인 필요";
  if (status === "RETRYABLE") return "자동으로 다시 처리 예정";
  return "직접 확인 필요";
}

function refundFailureReason(status: FailedRefundResponse["status"]): string {
  if (status === "RECONCILIATION_REQUIRED") return "결제사 환불 결과를 확인하지 못함";
  if (status === "RETRYABLE") return "일시적인 문제로 환불하지 못함";
  return "결제사에서 환불 요청을 완료하지 못함";
}

function refundTarget(refund: FailedRefundResponse): string {
  if (refund.orderClaimId != null) {
    return refund.orderId != null
      ? `주문 ${refund.orderId} · 교환·환불 요청 ${refund.orderClaimId}`
      : `교환·환불 요청 ${refund.orderClaimId}`;
  }
  if (refund.orderId != null) return `주문 ${refund.orderId}`;
  if (refund.bookingId != null) return `예약 ${refund.bookingId}`;
  if (refund.passPurchaseId != null) return `8회권 ${refund.passPurchaseId}`;
  if (refund.paymentAttemptId != null) return `결제 확인 번호 #${refund.paymentAttemptId}`;
  return "-";
}
