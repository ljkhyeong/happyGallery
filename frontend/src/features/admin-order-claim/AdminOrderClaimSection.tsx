import { useState } from "react";
import { Badge, Button, Col, Form, Row } from "react-bootstrap";
import {
  completeOrderClaimExchange,
  fetchAdminOrderClaims,
  resolveOrderClaim,
} from "./api";
import {
  ORDER_CLAIM_SLOW_POLL_INTERVAL_MS,
  orderClaimPollingInterval,
} from "@/features/order-claim/polling";
import type {
  CompleteOrderExchangeRequest,
  OrderClaimResolution,
  OrderClaimResponse,
  OrderClaimStatus,
  OrderClaimType,
  ResolveOrderClaimRequest,
} from "@/features/order-claim/types";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  initialStatus?: "" | OrderClaimStatus;
}

const TYPE_LABELS: Record<OrderClaimType, string> = {
  DAMAGED: "상품 파손·하자",
  WRONG_ITEM: "오배송",
  CHANGE_OF_MIND: "단순 변심",
  OTHER: "기타",
};

const RESOLUTION_LABELS: Record<OrderClaimResolution, string> = {
  REFUND: "환불",
  EXCHANGE: "교환",
};

const STATUS_OPTIONS: Array<{ value: "" | OrderClaimStatus; label: string }> = [
  { value: "", label: "전체" },
  { value: "REQUESTED", label: "접수" },
  { value: "REFUND_REQUESTED", label: "환불 처리 중" },
  { value: "EXCHANGE_APPROVED", label: "교환 승인" },
  { value: "REJECTED", label: "처리 거절" },
  { value: "COMPLETED", label: "완료" },
];

export function AdminOrderClaimSection({
  adminKey,
  onAuthError,
  initialStatus = "",
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [status, setStatus] = useState<"" | OrderClaimStatus>(initialStatus);
  const {
    cursor,
    hasPreviousPage,
    showNextPage,
    showPreviousPage,
    resetCursor,
  } = useCursorHistory();
  const [pendingId, setPendingId] = useState<number | null>(null);
  const queryKey = ["admin", "order-claims", status, cursor] as const;
  const { data: page, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchAdminOrderClaims(adminKey, status || undefined, cursor),
    refetchInterval: ({ state }) => status === "REQUESTED"
      ? ORDER_CLAIM_SLOW_POLL_INTERVAL_MS
      : orderClaimPollingInterval(
          state.data?.content,
          state.dataUpdateCount + state.fetchFailureCount,
        ),
  });
  const claims = page?.content;

  function changeStatus(nextStatus: "" | OrderClaimStatus) {
    setStatus(nextStatus);
    resetCursor();
  }

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["admin", "order-claims"] });
  }

  const resolve = useAdminMutation(onAuthError, {
    mutationFn: ({ claimId, body }: { claimId: number; body: ResolveOrderClaimRequest }) =>
      resolveOrderClaim(adminKey, claimId, body),
    onMutate: ({ claimId }) => setPendingId(claimId),
    onSuccess: (claim) => {
      toast.show(
        claim.status === "REJECTED"
          ? `주문 #${claim.orderId} 클레임을 거절했습니다.`
          : `주문 #${claim.orderId} ${RESOLUTION_LABELS[claim.requestedResolution]}을 승인했습니다.`,
      );
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });

  const completeExchange = useAdminMutation(onAuthError, {
    mutationFn: ({ claimId, body }: { claimId: number; body: CompleteOrderExchangeRequest }) =>
      completeOrderClaimExchange(adminKey, claimId, body),
    onMutate: ({ claimId }) => setPendingId(claimId),
    onSuccess: (claim) => {
      toast.show(`주문 #${claim.orderId} 교환 처리를 완료했습니다.`);
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });

  return (
    <div>
      <Row className="g-2 mb-3">
        <Col xs={12} sm={5}>
          <Form.Group controlId={`admin-order-claim-status-${initialStatus || "all"}`}>
            <Form.Label>처리 상태</Form.Label>
            <Form.Select
              value={status}
              onChange={(event) => changeStatus(
                STATUS_OPTIONS.find((option) => option.value === event.target.value)?.value ?? "",
              )}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>

      {isLoading && <LoadingSpinner />}
      {error && !(error instanceof ApiError && error.status === 401) && <ErrorAlert error={error} />}
      <ErrorAlert error={resolve.error ?? completeExchange.error} />
      {!isLoading && claims?.length === 0 && (
        <EmptyState message="해당 상태의 주문 클레임이 없습니다." />
      )}

      {claims?.map((claim) => (
        <AdminOrderClaimItem
          key={claim.id}
          claim={claim}
          pending={pendingId === claim.id}
          disabled={pendingId !== null}
          onResolve={(body) => resolve.mutate({ claimId: claim.id, body })}
          onCompleteExchange={(body) =>
            completeExchange.mutate({ claimId: claim.id, body })}
        />
      ))}
      {(hasPreviousPage || page?.hasMore) && (
        <div className="d-flex justify-content-center gap-2 mt-3">
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
    </div>
  );
}

interface ClaimItemProps {
  claim: OrderClaimResponse;
  pending: boolean;
  disabled: boolean;
  onResolve: (body: ResolveOrderClaimRequest) => void;
  onCompleteExchange: (body: CompleteOrderExchangeRequest) => void;
}

function AdminOrderClaimItem({
  claim,
  pending,
  disabled,
  onResolve,
  onCompleteExchange,
}: ClaimItemProps) {
  const [refundAmount, setRefundAmount] = useState(String(claim.maximumRefundAmount));
  const [restoreInventory, setRestoreInventory] = useState(false);
  const [note, setNote] = useState("");
  const [carrier, setCarrier] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");
  const parsedRefundAmount = Number(refundAmount);
  const validRefundAmount = Number.isSafeInteger(parsedRefundAmount)
    && parsedRefundAmount > 0
    && parsedRefundAmount <= claim.maximumRefundAmount;

  function approve() {
    onResolve({
      approved: true,
      refundAmount: claim.requestedResolution === "REFUND" ? parsedRefundAmount : undefined,
      restoreInventory,
      note: note.trim() || undefined,
    });
  }

  return (
    <article className="border rounded p-3 mb-3">
      <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
        <div className="d-flex flex-wrap gap-1 align-items-center">
          <strong>주문 #{claim.orderId} · 접수 #{claim.id}</strong>
          <Badge bg="secondary">{TYPE_LABELS[claim.type]}</Badge>
          <Badge bg="info">{RESOLUTION_LABELS[claim.requestedResolution]}</Badge>
          <Badge bg={claim.status === "REJECTED" ? "danger" : "success"}>
            {STATUS_OPTIONS.find((option) => option.value === claim.status)?.label}
          </Badge>
        </div>
        <small className="text-muted-soft">{formatDateTime(claim.requestedAt)}</small>
      </div>

      <ul className="small mb-2 ps-3">
        {claim.items.map((item) => (
          <li key={item.orderItemId}>
            {item.productName} {item.quantity}개 · {formatKRW(item.unitPrice * item.quantity)}
          </li>
        ))}
      </ul>
      <p className="small mb-2">{claim.customerReason}</p>
      <p className="small text-muted-soft mb-3">
        환불 가능 상한 {formatKRW(claim.maximumRefundAmount)}
      </p>

      {claim.status === "REQUESTED" && (
        <div>
          {claim.requestedResolution === "REFUND" && (
            <Form.Group className="mb-2" controlId={`admin-claim-${claim.id}-refund`}>
              <Form.Label>환불 금액</Form.Label>
              <Form.Control
                type="number"
                min={1}
                max={claim.maximumRefundAmount}
                value={refundAmount}
                disabled={disabled}
                isInvalid={refundAmount.length > 0 && !validRefundAmount}
                onChange={(event) => setRefundAmount(event.target.value)}
              />
            </Form.Group>
          )}
          <Form.Check
            type="switch"
            className="mb-2"
            id={`admin-claim-${claim.id}-inventory`}
            label="반품 수량 재고 복구"
            checked={restoreInventory}
            disabled={disabled}
            onChange={(event) => setRestoreInventory(event.target.checked)}
          />
          <Form.Group className="mb-2" controlId={`admin-claim-${claim.id}-note`}>
            <Form.Label>처리 메모</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              maxLength={1000}
              value={note}
              disabled={disabled}
              onChange={(event) => setNote(event.target.value)}
            />
          </Form.Group>
          <div className="d-flex flex-wrap gap-2">
            <Button
              size="sm"
              variant="success"
              disabled={disabled
                || (claim.requestedResolution === "REFUND" && !validRefundAmount)}
              onClick={approve}
            >
              {pending ? "처리 중..." : `${RESOLUTION_LABELS[claim.requestedResolution]} 승인`}
            </Button>
            <Button
              size="sm"
              variant="outline-danger"
              disabled={disabled || !note.trim()}
              onClick={() => onResolve({
                approved: false,
                restoreInventory: false,
                note: note.trim(),
              })}
            >
              {pending ? "처리 중..." : "거절"}
            </Button>
          </div>
        </div>
      )}

      {claim.status === "EXCHANGE_APPROVED" && (
        <div>
          <Row className="g-2 mb-2">
            <Col xs={12} sm={5}>
              <Form.Group controlId={`admin-claim-${claim.id}-carrier`}>
                <Form.Label>택배사</Form.Label>
                <Form.Control
                  maxLength={100}
                  value={carrier}
                  disabled={disabled}
                  onChange={(event) => setCarrier(event.target.value)}
                />
              </Form.Group>
            </Col>
            <Col xs={12} sm={7}>
              <Form.Group controlId={`admin-claim-${claim.id}-tracking`}>
                <Form.Label>운송장 번호</Form.Label>
                <Form.Control
                  maxLength={100}
                  value={trackingNumber}
                  disabled={disabled}
                  onChange={(event) => setTrackingNumber(event.target.value)}
                />
              </Form.Group>
            </Col>
          </Row>
          <Form.Group className="mb-2" controlId={`admin-claim-${claim.id}-complete-note`}>
            <Form.Label>완료 메모</Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              maxLength={1000}
              value={note}
              disabled={disabled}
              onChange={(event) => setNote(event.target.value)}
            />
          </Form.Group>
          <Button
            size="sm"
            variant="success"
            disabled={disabled || !carrier.trim() || !trackingNumber.trim()}
            onClick={() => onCompleteExchange({
              carrier: carrier.trim(),
              trackingNumber: trackingNumber.trim(),
              note: note.trim() || undefined,
            })}
          >
            {pending ? "처리 중..." : "교환 완료"}
          </Button>
        </div>
      )}

      {claim.adminNote && claim.status !== "REQUESTED" && (
        <p className="small text-muted-soft mb-1">처리 메모: {claim.adminNote}</p>
      )}
      {claim.refundAmount != null && (
        <p className="small mb-1">
          환불 {formatKRW(claim.refundAmount)}
          {claim.refundStatus ? ` · ${refundStatusLabel(claim.refundStatus)}` : ""}
        </p>
      )}
      {(claim.replacementCarrier || claim.replacementTrackingNumber) && (
        <p className="small mb-0">
          교환 배송: {[claim.replacementCarrier, claim.replacementTrackingNumber]
            .filter(Boolean)
            .join(" · ")}
        </p>
      )}
    </article>
  );
}

function refundStatusLabel(status: NonNullable<OrderClaimResponse["refundStatus"]>): string {
  switch (status) {
    case "REQUESTED": return "요청 접수";
    case "PROCESSING": return "처리 중";
    case "RETRYABLE": return "재시도 대기";
    case "RECONCILIATION_REQUIRED": return "상태 확인 필요";
    case "SUCCEEDED": return "환불 완료";
    case "FAILED": return "환불 실패";
  }
}
