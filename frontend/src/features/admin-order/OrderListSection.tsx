import { useState, useEffect, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { Table, Button, Form, Row, Col } from "react-bootstrap";
import type { AdminOrderResponse } from "@/shared/types";
import { fetchOrders } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { useOrderMutations } from "./useOrderMutations";
import { OrderActionCell } from "./OrderActionCell";
import { OrderHistoryPanel } from "./OrderHistoryPanel";

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

export function OrderListSection({ adminKey, onAuthError }: Props) {
  const [statusFilter, setStatusFilter] = useState("");
  const [historyOrderId, setHistoryOrderId] = useState<number | null>(null);
  const [allOrders, setAllOrders] = useState<AdminOrderResponse[]>([]);
  const [cursor, setCursor] = useState<string | undefined>(undefined);
  const [hasMore, setHasMore] = useState(false);

  const resetPagination = useCallback(() => {
    setAllOrders([]);
    setCursor(undefined);
    setHasMore(false);
  }, []);

  const mutations = useOrderMutations({ adminKey, onAuthError, onInvalidate: resetPagination });

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

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) onAuthError();
  }, [error, onAuthError]);

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
            disabled={mutations.expire.isPending}
            onClick={() => mutations.expire.mutate()}>
            {mutations.expire.isPending ? "처리 중..." : "픽업 만료 배치"}
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
                <th>주문번호</th><th>상태</th><th>금액</th><th>결제일</th><th>생성일</th><th>액션</th><th></th>
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
                  <td><OrderActionCell orderId={o.orderId} status={o.status} mutations={mutations} /></td>
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
        <OrderHistoryPanel orderId={historyOrderId} adminKey={adminKey} />
      )}

      {mutations.lastError && !(mutations.lastError instanceof ApiError && mutations.lastError.status === 401) && (
        <ErrorAlert error={mutations.lastError} />
      )}
    </div>
  );
}
