import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Table, Button, Badge, Form, Row, Col, InputGroup } from "react-bootstrap";
import {
  fetchOrders, approveOrder, rejectOrder, completeProduction,
  requestDelay, resumeProduction, preparePickup, completePickup, setExpectedShipDate, expirePickups,
} from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { OrderStatus } from "@/shared/types";

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
  { value: "PICKUP_READY", label: "픽업 대기" },
  { value: "PICKED_UP", label: "픽업 완료" },
  { value: "COMPLETED", label: "완료" },
  { value: "REJECTED", label: "거절" },
  { value: "AUTO_REFUND_TIMEOUT", label: "자동 환불" },
  { value: "PICKUP_EXPIRED", label: "픽업 만료" },
];

function statusBadge(status: OrderStatus) {
  const map: Record<string, { bg: string; label: string }> = {
    PAID_APPROVAL_PENDING: { bg: "warning", label: "승인 대기" },
    APPROVED_FULFILLMENT_PENDING: { bg: "info", label: "이행 대기" },
    IN_PRODUCTION: { bg: "primary", label: "제작 중" },
    DELAY_REQUESTED: { bg: "secondary", label: "지연 요청" },
    PICKUP_READY: { bg: "info", label: "픽업 대기" },
    PICKED_UP: { bg: "success", label: "픽업 완료" },
    COMPLETED: { bg: "success", label: "완료" },
    REJECTED: { bg: "danger", label: "거절" },
    AUTO_REFUND_TIMEOUT: { bg: "danger", label: "자동 환불" },
    PICKUP_EXPIRED: { bg: "danger", label: "픽업 만료" },
    SHIPPING_PREPARING: { bg: "info", label: "배송 준비" },
    SHIPPED: { bg: "primary", label: "배송 중" },
    DELIVERED: { bg: "success", label: "배송 완료" },
  };
  const m = map[status];
  return m ? <Badge bg={m.bg}>{m.label}</Badge> : <Badge bg="dark">{status}</Badge>;
}

export function OrderListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [statusFilter, setStatusFilter] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [pickupDeadline, setPickupDeadline] = useState<Record<number, string>>({});
  const [shipDate, setShipDate] = useState<Record<number, string>>({});

  const { data: orders, isLoading, error } = useQuery({
    queryKey: ["admin", "orders", statusFilter],
    queryFn: () => fetchOrders(adminKey, statusFilter || undefined),
  });

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onAuthError();
    }
  }, [error, onAuthError]);

  function onError(err: Error) {
    if (err instanceof ApiError && err.status === 401) onAuthError();
  }

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["admin", "orders", statusFilter] });
  }

  const approveMut = useMutation({
    mutationFn: (id: number) => approveOrder(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 승인 완료`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const rejectMut = useMutation({
    mutationFn: (id: number) => rejectOrder(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 거절 완료`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const completeProdMut = useMutation({
    mutationFn: (id: number) => completeProduction(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 제작 완료`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const delayMut = useMutation({
    mutationFn: (id: number) => requestDelay(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 지연 요청`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const pickupMut = useMutation({
    mutationFn: (id: number) => preparePickup(adminKey, id, { pickupDeadlineAt: pickupDeadline[id] || undefined }),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 픽업 준비 완료`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const pickupDoneMut = useMutation({
    mutationFn: (id: number) => completePickup(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 픽업 완료`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const shipDateMut = useMutation({
    mutationFn: (id: number) => setExpectedShipDate(adminKey, id, { expectedShipDate: shipDate[id] || undefined }),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 출고일 설정`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const resumeProdMut = useMutation({
    mutationFn: (id: number) => resumeProduction(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (_, id) => { toast.show(`주문 #${id} 제작 재개`); invalidate(); },
    onError,
    onSettled: () => setPendingId(null),
  });

  const expireMut = useMutation({
    mutationFn: () => expirePickups(adminKey),
    onSuccess: (r) => { toast.show(`픽업 만료 배치: 성공 ${r.successCount}, 실패 ${r.failureCount}`); invalidate(); },
    onError,
  });

  const lastMutError = approveMut.error || rejectMut.error || completeProdMut.error
    || delayMut.error || resumeProdMut.error || pickupMut.error || pickupDoneMut.error || shipDateMut.error || expireMut.error;

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
          <InputGroup size="sm" style={{ width: "auto" }}>
            <Form.Control type="datetime-local" value={pickupDeadline[orderId] || ""}
              onChange={(e) => setPickupDeadline(prev => ({ ...prev, [orderId]: e.target.value }))}
              style={{ maxWidth: 200 }} />
            <Button variant="outline-primary" disabled={disabled}
              onClick={() => pickupMut.mutate(orderId)}>
              {pending ? "..." : "픽업 준비"}
            </Button>
          </InputGroup>
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
            <Form.Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
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
      {orders && orders.length === 0 && <EmptyState message="해당 조건의 주문이 없습니다." />}

      {orders && orders.length > 0 && (
        <Table responsive hover size="sm">
          <thead>
            <tr>
              <th>주문번호</th>
              <th>상태</th>
              <th>금액</th>
              <th>결제일</th>
              <th>생성일</th>
              <th>액션</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.orderId}>
                <td>{o.orderNumber}</td>
                <td>{statusBadge(o.status)}</td>
                <td>{formatKRW(o.totalAmount)}</td>
                <td><small>{o.paidAt ? formatDateTime(o.paidAt) : "-"}</small></td>
                <td><small>{formatDateTime(o.createdAt)}</small></td>
                <td>{renderActions(o.orderId, o.status)}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      {lastMutError && !(lastMutError instanceof ApiError && lastMutError.status === 401) && (
        <ErrorAlert error={lastMutError} />
      )}
    </div>
  );
}
