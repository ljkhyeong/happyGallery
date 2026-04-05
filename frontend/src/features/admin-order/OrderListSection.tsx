import { useState, useEffect, useCallback } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Table, Button, Form, Row, Col, InputGroup } from "react-bootstrap";
import type { AdminOrderResponse, OrderStatus } from "@/shared/types";
import {
  fetchOrders, approveOrder, rejectOrder, completeProduction,
  requestDelay, resumeProduction, preparePickup, completePickup, setExpectedShipDate, expirePickups,
  prepareShipping, markShipped, markDelivered, fetchOrderHistory,
} from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { formatDateTime, formatKRW } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: "", label: "전체" },
  { value: "PAID_APPROVAL_PENDING", label: "승인 대기" },
  { value: "APPROVED_FULFILLMENT_PENDING", label: "이행 대기" },
  { value: "IN_PRODUCTION", label: "제작 중" },
  { value: "DELAY_REQUESTED", label: "지연 요청" },
  { value: "SHIPPING_PREPARING", label: "배송 준비" },
  { value: "SHIPPED", label: "배송 중" },
  { value: "DELIVERED", label: "배송 완료" },
  { value: "PICKUP_READY", label: "픽업 대기" },
  { value: "PICKED_UP", label: "픽업 완료" },
  { value: "COMPLETED", label: "완료" },
  { value: "REJECTED", label: "거절" },
  { value: "AUTO_REFUND_TIMEOUT", label: "자동 환불" },
  { value: "PICKUP_EXPIRED", label: "픽업 만료" },
];


const DECISION_LABELS: Record<string, string> = {
  APPROVE: "승인",
  REJECT: "거절",
  DELAY: "지연 요청",
  AUTO_REFUND: "자동 환불",
  PRODUCTION_COMPLETE: "제작 완료",
  RESUME_PRODUCTION: "제작 재개",
  PREPARE_SHIPPING: "배송 준비",
  SHIP: "배송 출발",
  DELIVER: "배송 완료",
};

function decisionLabel(decision: string): string {
  return DECISION_LABELS[decision] ?? decision;
}

export function OrderListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [statusFilter, setStatusFilter] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [pickupDeadline, setPickupDeadline] = useState<Record<number, string>>({});
  const [shipDate, setShipDate] = useState<Record<number, string>>({});
  const [historyOrderId, setHistoryOrderId] = useState<number | null>(null);
  const [allOrders, setAllOrders] = useState<AdminOrderResponse[]>([]);
  const [cursor, setCursor] = useState<string | undefined>(undefined);
  const [hasMore, setHasMore] = useState(false);

  const { data: page, isLoading, error, isFetching } = useQuery({
    queryKey: ["admin", "orders", statusFilter, cursor],
    queryFn: () => fetchOrders(adminKey, statusFilter || undefined, cursor),
  });

  useEffect(() => {
    if (page) {
      setAllOrders(prev => cursor ? [...prev, ...page.content] : page.content);
      setHasMore(page.hasMore);
    }
  }, [page, cursor]);

  const resetPagination = useCallback(() => {
    setAllOrders([]);
    setCursor(undefined);
    setHasMore(false);
  }, []);

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onAuthError();
    }
  }, [error, onAuthError]);

  function invalidate() {
    resetPagination();
    queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
  }

  const approveMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => approveOrder(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 승인 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const rejectMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => rejectOrder(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 거절 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const completeProdMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => completeProduction(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 제작 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const delayMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => requestDelay(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 지연 요청`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const pickupMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => preparePickup(adminKey, id, { pickupDeadlineAt: pickupDeadline[id] || undefined }),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 픽업 준비 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const pickupDoneMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => completePickup(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 픽업 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const shipDateMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => setExpectedShipDate(adminKey, id, { expectedShipDate: shipDate[id] || undefined }),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 출고일 설정`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const resumeProdMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => resumeProduction(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 제작 재개`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const prepareShipMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => prepareShipping(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 배송 준비`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const shippedMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => markShipped(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 배송 출발`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const deliveredMut = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => markDelivered(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 배송 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const { data: historyData, isLoading: historyLoading } = useQuery({
    queryKey: ["admin", "orders", historyOrderId, "history"],
    queryFn: () => fetchOrderHistory(adminKey, historyOrderId!),
    enabled: historyOrderId != null,
  });

  const expireMut = useAdminMutation(onAuthError, {
    mutationFn: () => expirePickups(adminKey),
    onSuccess: (r) => { toast.show(`픽업 만료 배치: 성공 ${r.successCount}, 실패 ${r.failureCount}`); invalidate(); },
  });

  const lastMutError = approveMut.error || rejectMut.error || completeProdMut.error
    || delayMut.error || resumeProdMut.error || pickupMut.error || pickupDoneMut.error || shipDateMut.error
    || prepareShipMut.error || shippedMut.error || deliveredMut.error || expireMut.error;

  function renderActions(orderId: number, status: OrderStatus) {
    const disabled = pendingId === orderId;
    const pending = pendingId === orderId;

    switch (status) {
      case "PAID_APPROVAL_PENDING":
        return (
          <div className="d-flex gap-1">
            <Button size="sm" variant="success" disabled={disabled}
              onClick={() => approveMut.mutate(orderId)}>
              {pending ? "..." : "승인"}
            </Button>
            <Button size="sm" variant="outline-danger" disabled={disabled}
              onClick={() => rejectMut.mutate(orderId)}>
              {pending ? "..." : "거절"}
            </Button>
          </div>
        );
      case "IN_PRODUCTION":
      case "DELAY_REQUESTED":
        return (
          <div className="d-flex gap-1 flex-wrap">
            <Button size="sm" variant="info" disabled={disabled}
              onClick={() => completeProdMut.mutate(orderId)}>
              {pending ? "..." : "제작 완료"}
            </Button>
            {status === "IN_PRODUCTION" && (
              <Button size="sm" variant="outline-warning" disabled={disabled}
                onClick={() => delayMut.mutate(orderId)}>
                {pending ? "..." : "지연"}
              </Button>
            )}
            {status === "DELAY_REQUESTED" && (
              <Button size="sm" variant="outline-success" disabled={disabled}
                onClick={() => resumeProdMut.mutate(orderId)}>
                {pending ? "..." : "재개"}
              </Button>
            )}
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="date" value={shipDate[orderId] || ""}
                onChange={(e) => setShipDate(prev => ({ ...prev, [orderId]: e.target.value }))}
                style={{ maxWidth: 150 }} />
              <Button variant="outline-primary" disabled={disabled}
                onClick={() => shipDateMut.mutate(orderId)}>출고일</Button>
            </InputGroup>
          </div>
        );
      case "APPROVED_FULFILLMENT_PENDING":
        return (
          <div className="d-flex gap-1 flex-wrap">
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="datetime-local" value={pickupDeadline[orderId] || ""}
                onChange={(e) => setPickupDeadline(prev => ({ ...prev, [orderId]: e.target.value }))}
                style={{ maxWidth: 200 }} />
              <Button variant="outline-primary" disabled={disabled}
                onClick={() => pickupMut.mutate(orderId)}>
                {pending ? "..." : "픽업 준비"}
              </Button>
            </InputGroup>
            <Button size="sm" variant="outline-info" disabled={disabled}
              onClick={() => prepareShipMut.mutate(orderId)}>
              {pending ? "..." : "배송 준비"}
            </Button>
          </div>
        );
      case "SHIPPING_PREPARING":
        return (
          <Button size="sm" variant="primary" disabled={disabled}
            onClick={() => shippedMut.mutate(orderId)}>
            {pending ? "..." : "배송 출발"}
          </Button>
        );
      case "SHIPPED":
        return (
          <Button size="sm" variant="success" disabled={disabled}
            onClick={() => deliveredMut.mutate(orderId)}>
            {pending ? "..." : "배송 완료"}
          </Button>
        );
      case "PICKUP_READY":
        return (
          <Button size="sm" variant="outline-success" disabled={disabled}
            onClick={() => pickupDoneMut.mutate(orderId)}>
            {pending ? "..." : "픽업 완료"}
          </Button>
        );
      default:
        return null;
    }
  }

  return (
    <div>
      <Row className="g-2 mb-3 align-items-end">
        <Col xs={12} sm={5}>
          <Form.Group controlId="admin-order-status-filter">
            <Form.Label>상태</Form.Label>
            <Form.Select value={statusFilter} onChange={(e) => { resetPagination(); setStatusFilter(e.target.value); }}>
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs="auto">
          <Button size="sm" variant="outline-secondary"
            disabled={expireMut.isPending}
            onClick={() => expireMut.mutate()}>
            {expireMut.isPending ? "처리 중..." : "픽업 만료 배치"}
          </Button>
        </Col>
      </Row>

      {isLoading && <LoadingSpinner />}
      {error && !(error instanceof ApiError && error.status === 401) && <ErrorAlert error={error} />}
      {!isLoading && allOrders.length === 0 && <EmptyState message="해당 조건의 주문이 없습니다." />}

      {allOrders.length > 0 && (
        <>
          <Table responsive hover size="sm">
            <thead>
              <tr>
                <th>주문번호</th>
                <th>상태</th>
                <th>금액</th>
                <th>결제일</th>
                <th>생성일</th>
                <th>액션</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {allOrders.map((o) => (
                <tr key={o.orderId}>
                  <td>{o.orderNumber}</td>
                  <td><StatusBadge status={o.status} /></td>
                  <td>{formatKRW(o.totalAmount)}</td>
                  <td><small>{o.paidAt ? formatDateTime(o.paidAt) : "-"}</small></td>
                  <td><small>{formatDateTime(o.createdAt)}</small></td>
                  <td>{renderActions(o.orderId, o.status)}</td>
                  <td>
                    <Button size="sm" variant="link"
                      onClick={() => setHistoryOrderId(historyOrderId === o.orderId ? null : o.orderId)}>
                      {historyOrderId === o.orderId ? "닫기" : "이력"}
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
          {hasMore && (
            <div className="text-center mb-3">
              <Button variant="outline-primary" size="sm"
                disabled={isFetching}
                onClick={() => page?.nextCursor && setCursor(page.nextCursor)}>
                {isFetching ? "불러오는 중..." : "더보기"}
              </Button>
            </div>
          )}
        </>
      )}

      {historyOrderId != null && (
        <div className="mt-3 p-3 border rounded">
          <h6>주문 #{historyOrderId} 이력</h6>
          {historyLoading && <LoadingSpinner />}
          {historyData && historyData.length === 0 && <small className="text-muted">이력이 없습니다.</small>}
          {historyData && historyData.length > 0 && (
            <Table size="sm" bordered>
              <thead>
                <tr>
                  <th>결정</th>
                  <th>관리자 ID</th>
                  <th>사유</th>
                  <th>일시</th>
                </tr>
              </thead>
              <tbody>
                {historyData.map((h) => (
                  <tr key={h.id}>
                    <td><small>{decisionLabel(h.decision)}</small></td>
                    <td><small>{h.decidedByAdminId ?? "-"}</small></td>
                    <td><small>{h.reason ?? "-"}</small></td>
                    <td><small>{formatDateTime(h.decidedAt)}</small></td>
                  </tr>
                ))}
              </tbody>
            </Table>
          )}
        </div>
      )}

      {lastMutError && !(lastMutError instanceof ApiError && lastMutError.status === 401) && (
        <ErrorAlert error={lastMutError} />
      )}
    </div>
  );
}
