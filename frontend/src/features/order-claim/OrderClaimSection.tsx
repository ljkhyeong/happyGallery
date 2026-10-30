import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Col, Form, Row } from "react-bootstrap";
import {
  fetchGuestOrderClaims,
  fetchMemberOrderClaims,
  requestGuestOrderClaim,
  requestMemberOrderClaim,
} from "./api";
import { ORDER_CLAIM_RESOLUTION_LABELS, ORDER_CLAIM_TYPE_LABELS } from "./labels";
import { orderClaimPollingInterval } from "./polling";
import type {
  OrderClaimRequest,
  OrderClaimResolution,
  OrderClaimResponse,
  OrderClaimStatus,
  OrderClaimType,
} from "./types";
import type { OrderDetailResponse } from "@/generated/api/order";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, ToneBadge, useToast, type CustomerBadgeTone } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

type ClaimAccess =
  | { kind: "member" }
  | { kind: "guest"; accessToken: string; requestKey: string };

interface Props {
  order: OrderDetailResponse;
  access: ClaimAccess;
}

interface ClaimStatusBadge {
  label: string;
  tone: CustomerBadgeTone;
}

// 공방이 처리 중인 접수는 먹색, 끝난 접수(완료·요청 거절)는 회색 면이다.
const STATUS_BADGES: Record<OrderClaimStatus, ClaimStatusBadge> = {
  REQUESTED: { label: "접수", tone: "progress" },
  REFUND_REQUESTED: { label: "환불 처리 중", tone: "progress" },
  EXCHANGE_APPROVED: { label: "교환 승인", tone: "progress" },
  REJECTED: { label: "요청 거절", tone: "done" },
  COMPLETED: { label: "완료", tone: "done" },
};

const CLAIMABLE_STATUSES = new Set(["DELIVERED", "PICKED_UP", "COMPLETED"]);

export function OrderClaimSection({ order, access }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [type, setType] = useState<"" | OrderClaimType>("");
  const [resolution, setResolution] = useState<"" | OrderClaimResolution>("");
  const [reason, setReason] = useState("");
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const queryKey = access.kind === "member"
    ? queryKeys.member.orders.claims(order.orderId)
    : queryKeys.guest.orderClaims(order.orderId, access.requestKey);
  const claimable = CLAIMABLE_STATUSES.has(order.status);

  const { data: claims, isLoading, isFetching, error, refetch } = useQuery({
    queryKey,
    queryFn: () => runForCurrentCustomer(
      () => access.kind === "member"
        ? fetchMemberOrderClaims(order.orderId)
        : fetchGuestOrderClaims(order.orderId, access.accessToken),
    ),
    enabled: claimable,
    refetchInterval: ({ state }) => orderClaimPollingInterval(
      state.data,
      state.dataUpdateCount + state.fetchFailureCount,
    ),
  });

  const claimedQuantities = useMemo(() => {
    const totals = new Map<number, number>();
    claims
      ?.filter((claim) => claim.status !== "REJECTED")
      .flatMap((claim) => claim.items)
      .forEach((item) => totals.set(
        item.orderItemId,
        (totals.get(item.orderItemId) ?? 0) + item.quantity,
      ));
    return totals;
  }, [claims]);

  const availableItems = order.items.map((item) => ({
    ...item,
    availableQuantity: Math.max(0, item.qty - (claimedQuantities.get(item.orderItemId) ?? 0)),
  }));
  const selectedItems = availableItems.flatMap((item) => {
    const quantity = quantities[item.orderItemId] ?? 0;
    return quantity > 0 ? [{ orderItemId: item.orderItemId, quantity }] : [];
  });
  const canSubmit = type !== ""
    && resolution !== ""
    && reason.trim().length > 0
    && selectedItems.length > 0;

  const applyClaimSuccess = async (requireCurrent: () => void) => {
    requireCurrent();
    await queryClient.invalidateQueries({ queryKey });
    requireCurrent();
    toast.show("반품·교환 요청을 접수했습니다.");
    setType("");
    setResolution("");
    setReason("");
    setQuantities({});
  };

  const requestClaim = useMutation({
    mutationFn: (body: OrderClaimRequest) => runForCurrentCustomer(
      () => access.kind === "member"
        ? requestMemberOrderClaim(order.orderId, body)
        : requestGuestOrderClaim(order.orderId, access.accessToken, body),
      async (result, requireCurrent) => {
        await applyClaimSuccess(requireCurrent);
        return result;
      },
    ),
  });

  if (!claimable) return null;

  function updateQuantity(orderItemId: number, quantity: number, maximum: number) {
    const boundedQuantity = Math.max(0, Math.min(quantity, maximum));
    setQuantities((current) => ({
      ...current,
      [orderItemId]: boundedQuantity,
    }));
  }

  function submit() {
    if (!type || !resolution || !canSubmit) return;
    requestClaim.mutate({
      type,
      requestedResolution: resolution,
      reason: reason.trim(),
      items: selectedItems,
    });
  }

  return (
    <section className="my-detail-page-section" aria-labelledby="order-claim-title">
      <h2 id="order-claim-title" className="my-detail-page-title mb-3">반품·교환</h2>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert
        error={error ?? requestClaim.error}
        onRetry={error ? () => { void refetch(); } : undefined}
        retrying={isFetching}
      />

      {!isLoading && claims && availableItems.some((item) => item.availableQuantity > 0) && (
        <Form className="mb-4" onSubmit={(event) => { event.preventDefault(); submit(); }}>
          <Row className="g-2 mb-3">
            <Col xs={12} sm={6}>
              <Form.Group controlId={`order-${order.orderId}-claim-type`}>
                <Form.Label>신청 사유</Form.Label>
                <Form.Select
                  value={type}
                  disabled={requestClaim.isPending}
                  onChange={(event) => setType(event.target.value as "" | OrderClaimType)}
                >
                  <option value="">선택</option>
                  {Object.entries(ORDER_CLAIM_TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col xs={12} sm={6}>
              <Form.Group controlId={`order-${order.orderId}-claim-resolution`}>
                <Form.Label>환불·교환 선택</Form.Label>
                <Form.Select
                  value={resolution}
                  disabled={requestClaim.isPending}
                  onChange={(event) =>
                    setResolution(event.target.value as "" | OrderClaimResolution)}
                >
                  <option value="">선택</option>
                  {Object.entries(ORDER_CLAIM_RESOLUTION_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>

          <fieldset className="mb-3">
            <legend className="fs-6">대상 작품</legend>
            {availableItems.map((item) => {
              const quantity = quantities[item.orderItemId] ?? 0;
              return (
                <div
                  key={item.orderItemId}
                  className="d-flex flex-wrap align-items-center justify-content-between gap-2 py-2 border-bottom"
                >
                  <Form.Check
                    id={`order-${order.orderId}-claim-item-${item.orderItemId}`}
                    label={`${item.productName} · 접수 가능 ${item.availableQuantity}개`}
                    checked={quantity > 0}
                    disabled={item.availableQuantity === 0 || requestClaim.isPending}
                    onChange={(event) =>
                      updateQuantity(
                        item.orderItemId,
                        event.target.checked ? 1 : 0,
                        item.availableQuantity,
                      )}
                  />
                  {quantity > 0 && (
                    <Form.Control
                      type="number" inputMode="numeric"
                      min={1}
                      max={item.availableQuantity}
                      value={quantity}
                      aria-label={`${item.productName} 접수 수량`}
                      disabled={requestClaim.isPending}
                      style={{ width: 88 }}
                      onChange={(event) =>
                        updateQuantity(
                          item.orderItemId,
                          Number(event.target.value),
                          item.availableQuantity,
                        )}
                    />
                  )}
                </div>
              );
            })}
          </fieldset>

          <Form.Group className="mb-3" controlId={`order-${order.orderId}-claim-reason`}>
            <Form.Label>상세 사유</Form.Label>
            <Form.Control
              as="textarea"
              rows={4}
              maxLength={1000}
              value={reason}
              disabled={requestClaim.isPending}
              onChange={(event) => setReason(event.target.value)}
            />
            <Form.Text className="text-muted">{reason.length}/1000자</Form.Text>
          </Form.Group>

          <Button type="submit" disabled={!canSubmit || requestClaim.isPending}>
            {requestClaim.isPending ? "접수 중..." : "반품·교환 접수"}
          </Button>
        </Form>
      )}

      {claims && claims.length > 0 && (
        <div>
          <h3 className="h6">접수 내역</h3>
          {claims.map((claim) => <OrderClaimHistoryItem key={claim.id} claim={claim} />)}
        </div>
      )}
    </section>
  );
}

function OrderClaimHistoryItem({ claim }: { claim: OrderClaimResponse }) {
  const status: ClaimStatusBadge = STATUS_BADGES[claim.status] ?? { label: "상태 확인 필요", tone: "done" };
  return (
    <article className="border rounded p-3 mb-2">
      <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
        <div className="d-flex flex-wrap gap-1 align-items-center">
          <strong>접수 #{claim.id}</strong>
          <Badge bg="light">{ORDER_CLAIM_TYPE_LABELS[claim.type]}</Badge>
          <Badge bg="light">{ORDER_CLAIM_RESOLUTION_LABELS[claim.requestedResolution]}</Badge>
          <ToneBadge tone={status.tone}>{status.label}</ToneBadge>
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
      <p className="small mb-1">{claim.customerReason}</p>
      {claim.adminNote && <p className="small text-muted-soft mb-1">처리 메모: {claim.adminNote}</p>}
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
