import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Col, Form, Row } from "react-bootstrap";
import {
  fetchGuestOrderClaims,
  fetchMemberOrderClaims,
  requestGuestOrderClaim,
  requestMemberOrderClaim,
} from "./api";
import { orderClaimPollingInterval } from "./polling";
import type {
  OrderClaimRequest,
  OrderClaimResolution,
  OrderClaimResponse,
  OrderClaimStatus,
  OrderClaimType,
} from "./types";
import type { OrderDetailResponse } from "@/shared/types";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

type ClaimAccess =
  | { kind: "member" }
  | { kind: "guest"; accessToken: string; requestKey: string };

interface Props {
  order: OrderDetailResponse;
  access: ClaimAccess;
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

const STATUS_LABELS: Record<OrderClaimStatus, string> = {
  REQUESTED: "접수",
  REFUND_REQUESTED: "환불 처리 중",
  EXCHANGE_APPROVED: "교환 승인",
  REJECTED: "처리 거절",
  COMPLETED: "완료",
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
    : ["guest", "order", order.orderId, "claims", access.requestKey] as const;
  const claimable = CLAIMABLE_STATUSES.has(order.status);

  const { data: claims = [], isLoading, error } = useQuery({
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
      .filter((claim) => claim.status !== "REJECTED")
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
    <section className="mt-4 border-top pt-4" aria-labelledby="order-claim-title">
      <h5 id="order-claim-title">반품·교환</h5>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error ?? requestClaim.error} />

      {!isLoading && availableItems.some((item) => item.availableQuantity > 0) && (
        <Form className="mb-4" onSubmit={(event) => { event.preventDefault(); submit(); }}>
          <Row className="g-2 mb-3">
            <Col xs={12} sm={6}>
              <Form.Group controlId={`order-${order.orderId}-claim-type`}>
                <Form.Label>접수 유형</Form.Label>
                <Form.Select
                  value={type}
                  disabled={requestClaim.isPending}
                  onChange={(event) => setType(event.target.value as "" | OrderClaimType)}
                >
                  <option value="">선택</option>
                  {Object.entries(TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
            <Col xs={12} sm={6}>
              <Form.Group controlId={`order-${order.orderId}-claim-resolution`}>
                <Form.Label>요청 방법</Form.Label>
                <Form.Select
                  value={resolution}
                  disabled={requestClaim.isPending}
                  onChange={(event) =>
                    setResolution(event.target.value as "" | OrderClaimResolution)}
                >
                  <option value="">선택</option>
                  {Object.entries(RESOLUTION_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>

          <fieldset className="mb-3">
            <legend className="fs-6">대상 상품</legend>
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
                      type="number"
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
            <Form.Label>접수 사유</Form.Label>
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

      {claims.length > 0 && (
        <div>
          <h6>접수 내역</h6>
          {claims.map((claim) => <OrderClaimHistoryItem key={claim.id} claim={claim} />)}
        </div>
      )}
    </section>
  );
}

function OrderClaimHistoryItem({ claim }: { claim: OrderClaimResponse }) {
  return (
    <article className="border rounded p-3 mb-2">
      <div className="d-flex flex-wrap justify-content-between gap-2 mb-2">
        <div className="d-flex flex-wrap gap-1 align-items-center">
          <strong>접수 #{claim.id}</strong>
          <Badge bg="secondary">{TYPE_LABELS[claim.type]}</Badge>
          <Badge bg="info">{RESOLUTION_LABELS[claim.requestedResolution]}</Badge>
          <Badge bg={claim.status === "REJECTED" ? "danger" : "success"}>
            {STATUS_LABELS[claim.status]}
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
