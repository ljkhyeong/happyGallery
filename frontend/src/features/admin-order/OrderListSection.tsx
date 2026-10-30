import { useState } from "react";
import { Table, Button, Form, Row, Col } from "react-bootstrap";
import { X } from "lucide-react";
import type { OrderStatus } from "@/shared/types";
import { fetchOrderFulfillment, fetchOrders } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { formatDate, formatKRW, formatTime } from "@/shared/lib";
import { parseEnumValue } from "@/shared/lib/enum";
import { ListOrdersStatus } from "@/generated/api/adminOrder";
import { useOrderMutations } from "./useOrderMutations";
import { visibleAdminError } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminInfiniteQuery } from "@/shared/hooks/useAdminInfiniteQuery";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { OrderActionCell } from "./OrderActionCell";
import { RiskActionButton } from "./RiskActionButton";
import { OrderHistoryPanel } from "./OrderHistoryPanel";
import { OrderFulfillmentDetails, OrderFulfillmentPanel } from "./OrderFulfillmentPanel";
import { ProductPurchaseTerms } from "@/features/product/ProductPurchaseTerms";
import { OrderOptionList } from "@/features/order/OrderOptionList";
import { queryKeys } from "@/shared/api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  initialStatus?: "" | ListOrdersStatus;
  focusOrderId?: number;
  focusOrderStatus?: OrderStatus;
}

const STATUS_OPTIONS: ReadonlyArray<{ value: "" | ListOrdersStatus; label: string }> = [
  { value: "", label: "전체" },
  { value: "PAID_APPROVAL_PENDING", label: "승인 대기" },
  { value: "APPROVED_FULFILLMENT_PENDING", label: "배송·수령 준비 대기" },
  { value: "IN_PRODUCTION", label: "제작 중" },
  { value: "DELAY_CONSENT_PENDING", label: "제작 지연 고객 답변 대기" },
  { value: "DELAY_ACCEPTED", label: "고객이 제작 지연에 동의" },
  { value: "DELAY_REJECTED_CANCELED", label: "고객 거절로 취소" },
  { value: "SHIPPING_PREPARING", label: "배송 준비" },
  { value: "SHIPPED", label: "배송 중" },
  { value: "DELIVERED", label: "배송 완료" },
  { value: "PICKUP_READY", label: "매장 수령 대기" },
  { value: "PICKED_UP", label: "매장 수령 완료" },
  { value: "COMPLETED", label: "완료" },
  { value: "REJECTED", label: "거절" },
  { value: "CUSTOMER_CANCELED", label: "고객 취소" },
  { value: "AUTO_REFUND_TIMEOUT", label: "자동 환불" },
  { value: "PICKUP_EXPIRED", label: "미수령 환불 처리" },
  { value: "PICKUP_FORFEITED", label: "미수령 종료(환불 없음)" },
];

/** 좁은 칸에서 날짜·시각이 여러 줄로 꺾이지 않게 날짜와 시각을 한 줄씩 둔다. */
function DateTimeLines({ value }: { value: string | null | undefined }) {
  if (!value) return <small>-</small>;
  return (
    <small className="d-block">
      <span className="d-block text-nowrap">{formatDate(value)}</span>
      <span className="d-block text-nowrap">{formatTime(value)}</span>
    </small>
  );
}

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
  const mutations = useOrderMutations({
    adminKey,
    onAuthError,
    // 처리 뒤에는 주소에서 받은 상태가 맞지 않으므로 검색한 주문 영역을 닫는다. 목록은 불러온 페이지째 다시 읽는다.
    onInvalidate: () => setFocusedOrder(null),
  });

  const focusedFulfillment = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.orders.fulfillment(focusedOrder?.id),
    queryFn: () => fetchOrderFulfillment(adminKey, focusedOrder!.id),
    enabled: focusedOrder != null,
  });

  const {
    data,
    isLoading,
    error,
    isFetching,
    refetch,
    hasNextPage,
    fetchNextPage,
    isFetchNextPageError,
  } = useAdminInfiniteQuery(onAuthError, {
    queryKey: queryKeys.admin.orders.page(statusFilter),
    queryFn: ({ pageParam }) => fetchOrders(adminKey, statusFilter || undefined, pageParam),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => (lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined),
  });
  const orders = data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <div>
      {focusedOrder && (
        <section className="mb-4 border-bottom pb-3" aria-labelledby="focused-order-title">
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
            <div className="d-flex align-items-center gap-2">
              <h6 id="focused-order-title" className="mb-0">검색한 주문 #{focusedOrder.id}</h6>
              <StatusBadge status={focusedOrder.status} audience="admin" />
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
            <Form.Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(parseEnumValue(ListOrdersStatus, e.target.value) ?? "")}
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs="auto">
          <RiskActionButton
            variant="outline-secondary"
            size="default"
            buttonLabel="수령 기한 지난 주문 미수령 처리"
            confirmLabel="미수령 처리 실행"
            title="수령 기한 지난 주문 처리 확인"
            impact="모든 주문을 환불 없이 미수령으로 종료합니다. 기성품 재고는 복구하고 주문제작 재고는 유지합니다. 고객과 상담 후 환불이 필요하면 개별 주문의 ‘관리자 예외 환불’을 사용하세요."
            disabled={mutations.expire.isPending}
            pending={mutations.expire.isPending}
            onConfirm={() => mutations.expire.mutate()}
          />
        </Col>
      </Row>

      {isLoading && <LoadingSpinner />}
      {visibleAdminError(error) && (
        <ErrorAlert
          error={error}
          onRetry={() => { void (isFetchNextPageError ? fetchNextPage() : refetch()); }}
          retrying={isFetching}
        />
      )}
      {!isLoading && !error && data && orders.length === 0 && (
        <EmptyState message="해당 조건의 주문이 없습니다." />
      )}

      {orders.length > 0 && (
        <>
          <Table responsive hover size="sm">
            <thead>
              <tr>
                <th>주문번호</th><th>상품</th><th>상태</th><th>수령</th><th>금액</th><th>결제일</th><th>생성일</th><th>처리</th>
              </tr>
            </thead>
            <tbody>
              {orders.map((o) => (
                <tr key={o.orderId}>
                  <td className="text-nowrap">{o.orderNumber}</td>
                  <td>
                    {o.items.map((item) => (
                      <div key={`${item.productId}-${item.productVariantId ?? 0}`} className="mb-2">
                        <small>{item.productName} x {item.qty}</small>
                        <OrderOptionList options={item.options} />
                        <ProductPurchaseTerms
                          productName={item.productName}
                          type={item.productType}
                          specification={item.specification}
                          careInstructions={item.careInstructions}
                          productionLeadDays={item.productionLeadDays}
                          compact
                          showCustomizationInquiry={false}
                        />
                      </div>
                    ))}
                  </td>
                  <td><StatusBadge status={o.status} audience="admin" /></td>
                  <td className="text-nowrap">{o.fulfillmentType === "SHIPPING" ? "택배" : o.fulfillmentType === "PICKUP" ? "매장 수령" : "-"}</td>
                  <td className="text-nowrap">{formatKRW(o.totalAmount)}</td>
                  <td><DateTimeLines value={o.paidAt} /></td>
                  <td><DateTimeLines value={o.createdAt} /></td>
                  <td>
                    <OrderActionCell orderId={o.orderId} status={o.status}
                      fulfillmentType={o.fulfillmentType} mutations={mutations} />
                    {/* 따로 칸을 두면 태블릿 폭에서 표가 넘쳐, 처리 버튼 아래에 세로로 둔다. */}
                    <div className="d-flex flex-column align-items-start mt-1">
                      <Button size="sm" variant="link" className="text-nowrap px-0"
                        aria-expanded={fulfillmentOrderId === o.orderId}
                        aria-controls={`order-fulfillment-${o.orderId}`}
                        onClick={() => setFulfillmentOrderId(fulfillmentOrderId === o.orderId ? null : o.orderId)}>
                        {fulfillmentOrderId === o.orderId ? "배송·수령 정보 닫기" : "배송·수령 정보"}
                      </Button>
                      <Button size="sm" variant="link" className="text-nowrap px-0"
                        aria-expanded={historyOrderId === o.orderId}
                        aria-controls={`order-history-${o.orderId}`}
                        onClick={() => setHistoryOrderId(historyOrderId === o.orderId ? null : o.orderId)}>
                        {historyOrderId === o.orderId ? "주문 처리 이력 닫기" : "주문 처리 이력"}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
          {hasNextPage && (
            <div className="text-center mb-3">
              <Button variant="outline-primary" size="sm"
                disabled={isFetching}
                onClick={() => void fetchNextPage()}>
                {isFetching ? "불러오는 중..." : "더보기"}
              </Button>
            </div>
          )}
        </>
      )}

      {historyOrderId != null && (
        <div id={`order-history-${historyOrderId}`}>
          <OrderHistoryPanel
            orderId={historyOrderId}
            adminKey={adminKey}
            onAuthError={onAuthError}
          />
        </div>
      )}

      {fulfillmentOrderId != null && (
        <div id={`order-fulfillment-${fulfillmentOrderId}`}>
          <OrderFulfillmentPanel
            orderId={fulfillmentOrderId}
            adminKey={adminKey}
            onAuthError={onAuthError}
          />
        </div>
      )}

      <ErrorAlert error={visibleAdminError(mutations.lastError)} />
    </div>
  );
}
