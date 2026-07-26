import { useState, useEffect } from "react";
import { Table, Button, Form, Row, Col } from "react-bootstrap";
import { X } from "lucide-react";
import type { AdminOrderResponse, OrderStatus } from "@/shared/types";
import { fetchOrderFulfillment, fetchOrders } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { useOrderMutations } from "./useOrderMutations";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { OrderActionCell } from "./OrderActionCell";
import { OrderHistoryPanel } from "./OrderHistoryPanel";
import { OrderFulfillmentDetails, OrderFulfillmentPanel } from "./OrderFulfillmentPanel";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  initialStatus?: string;
  focusOrderId?: number;
  focusOrderStatus?: OrderStatus;
}

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: "", label: "전체" },
  { value: "PAID_APPROVAL_PENDING", label: "승인 대기" },
  { value: "APPROVED_FULFILLMENT_PENDING", label: "이행 대기" },
  { value: "IN_PRODUCTION", label: "제작 중" },
  { value: "DELAY_CONSENT_PENDING", label: "지연 응답 대기" },
  { value: "DELAY_ACCEPTED", label: "지연 수락" },
  { value: "DELAY_REJECTED_CANCELED", label: "지연 거절 취소" },
  { value: "SHIPPING_PREPARING", label: "배송 준비" },
  { value: "SHIPPED", label: "배송 중" },
  { value: "DELIVERED", label: "배송 완료" },
  { value: "PICKUP_READY", label: "픽업 대기" },
  { value: "PICKED_UP", label: "픽업 완료" },
  { value: "COMPLETED", label: "완료" },
  { value: "REJECTED", label: "거절" },
  { value: "CUSTOMER_CANCELED", label: "고객 취소" },
  { value: "AUTO_REFUND_TIMEOUT", label: "자동 환불" },
  { value: "PICKUP_EXPIRED", label: "픽업 만료 환불" },
  { value: "PICKUP_FORFEITED", label: "픽업 미수령 종료" },
];

export function OrderListSection({
  adminKey,
  onAuthError,
  initialStatus = "",
  focusOrderId,
  focusOrderStatus,
}: Props) {
  const [statusFilter, setStatusFilter] = useState(initialStatus);
  const [historyOrderId, setHistoryOrderId] = useState<number | null>(null);
  const [fulfillmentOrderId, setFulfillmentOrderId] = useState<number | null>(null);
  const [focusedOrder, setFocusedOrder] = useState(
    focusOrderId != null && focusOrderStatus
      ? { id: focusOrderId, status: focusOrderStatus }
      : null,
  );
  const [allOrders, setAllOrders] = useState<AdminOrderResponse[]>([]);
  const [cursor, setCursor] = useState<string | undefined>(undefined);
  const [hasMore, setHasMore] = useState(false);

  function resetPagination() {
    setAllOrders([]);
    setCursor(undefined);
    setHasMore(false);
  }

  const mutations = useOrderMutations({
    adminKey,
    onAuthError,
    onInvalidate: () => {
      resetPagination();
      setFocusedOrder(null);
    },
  });

  const focusedFulfillment = useAdminQuery(onAuthError, {
    queryKey: ["admin", "orders", focusedOrder?.id, "fulfillment"],
    queryFn: () => fetchOrderFulfillment(adminKey, focusedOrder!.id),
    enabled: focusedOrder != null,
  });

  const { data: page, isLoading, error, isFetching } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "orders", statusFilter, cursor],
    queryFn: () => fetchOrders(adminKey, statusFilter || undefined, cursor),
  });

  useEffect(() => {
    if (page) {
      setAllOrders(prev => cursor ? [...prev, ...page.content] : page.content);
      setHasMore(page.hasMore);
    }
  }, [page, cursor]);

  return (
    <div>
      {focusedOrder && (
        <section className="mb-4 border-bottom pb-3" aria-labelledby="focused-order-title">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
            <div className="d-flex align-items-center gap-2">
              <h6 id="focused-order-title" className="mb-0">검색한 주문 #{focusedOrder.id}</h6>
              <StatusBadge status={focusedOrder.status} />
            </div>
            <Button
              size="sm"
              variant="outline-secondary"
              aria-label="검색한 주문 닫기"
              title="닫기"
              onClick={() => setFocusedOrder(null)}
            >
              <X size={14} aria-hidden="true" />
            </Button>
          </div>
          {focusedFulfillment.isLoading && <LoadingSpinner />}
          {focusedFulfillment.error && <ErrorAlert error={focusedFulfillment.error} />}
          {focusedFulfillment.data && (
            <>
              <div className="mb-3">
                <OrderActionCell
                  orderId={focusedOrder.id}
                  status={focusedOrder.status}
                  fulfillmentType={focusedFulfillment.data.type}
                  mutations={mutations}
                />
              </div>
              <OrderFulfillmentDetails fulfillment={focusedFulfillment.data} />
              <OrderHistoryPanel
                orderId={focusedOrder.id}
                adminKey={adminKey}
                onAuthError={onAuthError}
              />
            </>
          )}
        </section>
      )}

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
                <th>주문번호</th><th>상품</th><th>상태</th><th>수령</th><th>금액</th><th>결제일</th><th>생성일</th><th>액션</th><th></th>
              </tr>
            </thead>
            <tbody>
              {allOrders.map((o) => (
                <tr key={o.orderId}>
                  <td>{o.orderNumber}</td>
                  <td>
                    {o.items.map((item) => (
                      <div key={item.productId} className="mb-2">
                        <small>{item.productName} x {item.qty}</small>
                        <ProductPurchaseTerms
                          productName={item.productName}
                          type={item.productType}
                          specification={item.specification}
                          careInstructions={item.careInstructions}
                          productionLeadDays={item.productionLeadDays}
                          compact
                          showCustomizationInquiry={false}
                          showLegacySnapshotNotice
                        />
                      </div>
                    ))}
                  </td>
                  <td><StatusBadge status={o.status} /></td>
                  <td>{o.fulfillmentType === "SHIPPING" ? "택배" : o.fulfillmentType === "PICKUP" ? "픽업" : "-"}</td>
                  <td>{formatKRW(o.totalAmount)}</td>
                  <td><small>{o.paidAt ? formatDateTime(o.paidAt) : "-"}</small></td>
                  <td><small>{formatDateTime(o.createdAt)}</small></td>
                  <td><OrderActionCell orderId={o.orderId} status={o.status}
                    fulfillmentType={o.fulfillmentType} mutations={mutations} /></td>
                  <td>
                    <Button size="sm" variant="link"
                      onClick={() => setFulfillmentOrderId(fulfillmentOrderId === o.orderId ? null : o.orderId)}>
                      {fulfillmentOrderId === o.orderId ? "닫기" : "이행"}
                    </Button>
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
        <OrderHistoryPanel
          orderId={historyOrderId}
          adminKey={adminKey}
          onAuthError={onAuthError}
        />
      )}

      {fulfillmentOrderId != null && (
        <OrderFulfillmentPanel
          orderId={fulfillmentOrderId}
          adminKey={adminKey}
          onAuthError={onAuthError}
        />
      )}

      {mutations.lastError && !(mutations.lastError instanceof ApiError && mutations.lastError.status === 401) && (
        <ErrorAlert error={mutations.lastError} />
      )}
    </div>
  );
}
