import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Col, Form, Modal, Row, Table } from "react-bootstrap";
import type {
  DelaySmartStoreOrderRequest,
  DispatchSmartStoreExchangeRequest,
  DispatchSmartStoreOrderRequest,
  SmartStoreChannelOrderResponse,
  SmartStoreChannelOrderResponseAttentionReason,
  HoldSmartStoreExchangeRequest,
  HoldSmartStoreReturnRequest,
  ListSmartStoreChannelOrdersAttentionReason,
  RequestSmartStoreSellerCancelRequest,
  RequestSmartStoreSellerReturnRequest,
  ResolveSmartStoreInventoryRequestAction,
  SmartStoreOrderActionHistoryResponse,
  SmartStoreOrderBulkActionResponse,
} from "@/generated/api/adminOrder";
import type { ProductResponse, ProductVariantResponse } from "@/generated/api/adminCatalog";
import { ApiError } from "@/shared/api";
import { fetchProducts } from "@/features/admin-product/api";
import { ReturnDeliveryCompanies } from "./ReturnDeliveryCompanies";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LinkButton, LoadingSpinner, useToast } from "@/shared/ui";
import {
  approveSmartStoreCancel,
  approveSmartStoreReturn,
  confirmSmartStoreOrder,
  confirmSmartStoreOrders,
  delaySmartStoreOrder,
  dispatchSmartStoreExchange,
  dispatchSmartStoreOrder,
  dispatchSmartStoreOrders,
  completeSmartStoreExchangeCollection,
  holdSmartStoreExchange,
  holdSmartStoreReturn,
  releaseSmartStoreExchange,
  releaseSmartStoreReturn,
  rejectSmartStoreExchange,
  requestSmartStoreOrderCancel,
  requestSmartStoreOrderReturn,
  fetchSmartStoreChannelOrderActions,
  fetchSmartStoreChannelOrder,
  fetchSmartStoreChannelOrders,
  rejectSmartStoreReturn,
  resolveSmartStoreOrderInventory,
  resolveSmartStoreReturn,
  retrySmartStoreOrderInventory,
} from "./api";
import { ACTION_LABELS, ACTION_STATUS_LABELS } from "./labels";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  attentionOnly?: boolean;
  focusProductOrderId?: string;
}

const ATTENTION_LABELS: Record<
  Exclude<SmartStoreChannelOrderResponseAttentionReason, null>,
  string
> = {
  MAPPING_REQUIRED: "상품·옵션 매핑 필요",
  STOCK_SHORTAGE: "내부 재고 부족",
  RETURN_REVIEW: "반품 검수 필요",
  STATUS_REVIEW: "새 주문 상태 확인 필요",
};

const STATUS_LABELS: Record<string, string> = {
  PAYMENT_WAITING: "결제 대기",
  PAYED: "결제 완료",
  DELIVERING: "배송 중",
  DELIVERED: "배송 완료",
  PURCHASE_DECIDED: "구매 확정",
  EXCHANGED: "교환 완료",
  CANCELED: "취소",
  RETURNED: "반품 완료",
  CANCELED_BY_NOPAYMENT: "미결제 취소",
};

const MAX_BULK_ORDERS = 30;

export function SmartStoreChannelOrderSection({
  adminKey,
  onAuthError,
  attentionOnly: initialAttentionOnly = false,
  focusProductOrderId,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [attentionOnly, setAttentionOnly] = useState(initialAttentionOnly);
  const [attentionReason, setAttentionReason] = useState<
    ListSmartStoreChannelOrdersAttentionReason | ""
  >("");
  const [pageCursors, setPageCursors] = useState<(string | undefined)[]>([undefined]);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(focusProductOrderId ?? null);
  const [inventoryResolutionOrder, setInventoryResolutionOrder] =
    useState<SmartStoreChannelOrderResponse | null>(null);
  const [returnReviewOrder, setReturnReviewOrder] = useState<SmartStoreChannelOrderResponse | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkDispatchOpen, setBulkDispatchOpen] = useState(false);
  const [bulkResult, setBulkResult] = useState<SmartStoreOrderBulkActionResponse | null>(null);
  const cursor = pageCursors.at(-1);
  const queryKey = [
    "admin", "smartstore-orders", attentionOnly, attentionReason || null, cursor ?? null,
  ] as const;
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchSmartStoreChannelOrders(
      adminKey,
      attentionOnly,
      attentionReason || undefined,
      cursor,
    ),
    refetchInterval: 30_000,
  });
  const orders = data?.content ?? [];

  function resetPages() {
    setPageCursors([undefined]);
    setSelectedIds(new Set());
  }

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ["admin", "smartstore-orders"] });
  }

  const retryInventory = useAdminMutation(onAuthError, {
    mutationFn: (productOrderId: string) =>
      retrySmartStoreOrderInventory(adminKey, productOrderId),
    onMutate: setPendingId,
    onSuccess: (order) => {
      toast.show(order.attentionReason
        ? "재고를 반영하지 못했습니다. 확인 사유를 검토해 주세요."
        : "스마트스토어 주문 재고를 반영했습니다.");
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });

  const resolveReturn = useAdminMutation(onAuthError, {
    mutationFn: ({ order, restoreStock }: {
      order: SmartStoreChannelOrderResponse;
      restoreStock: boolean;
    }) => resolveSmartStoreReturn(adminKey, order.productOrderId, {
      restoreStock,
      reviewVersion: order.returnReviewVersion,
    }),
    onMutate: ({ order }) => setPendingId(order.productOrderId),
    onSuccess: (_, variables) => {
      toast.show(variables.restoreStock
        ? "반품 검수 수량을 내부 재고에 복원했습니다."
        : "판매 불가 반품으로 확인하고 재고를 복원하지 않았습니다.");
      setReturnReviewOrder(null);
      invalidate();
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409) {
        setReturnReviewOrder(null);
        invalidate();
      }
    },
    onSettled: () => setPendingId(null),
  });
  const bulkConfirm = useAdminMutation(onAuthError, {
    mutationFn: () => confirmSmartStoreOrders(adminKey, [...selectedIds]),
    onSuccess: (result) => {
      setBulkResult(result);
      setSelectedIds(new Set(result.failures.map((failure) => failure.productOrderId)));
      toast.show(`발주 확인 ${result.successProductOrderIds.length}건을 요청했습니다.`);
      invalidate();
    },
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }

  return (
    <>
      {!initialAttentionOnly && <ReturnDeliveryCompanies adminKey={adminKey} onAuthError={onAuthError} />}
      {!initialAttentionOnly && (
        <Form.Check
          className="mb-3"
          type="switch"
          id="smartstore-order-attention-only"
          label="확인이 필요한 주문만 보기"
          checked={attentionOnly}
          onChange={(event) => {
            setAttentionOnly(event.target.checked);
            if (!event.target.checked) setAttentionReason("");
            resetPages();
          }}
        />
      )}
      {attentionOnly && (
        <Form.Select
          className="mb-3"
          style={{ maxWidth: 300 }}
          aria-label="확인 사유 필터"
          value={attentionReason}
          onChange={(event) => {
            setAttentionReason(event.target.value as ListSmartStoreChannelOrdersAttentionReason | "");
            resetPages();
          }}
        >
          <option value="">모든 확인 사유</option>
          <option value="MAPPING_REQUIRED">상품·옵션 매핑 필요</option>
          <option value="STOCK_SHORTAGE">내부 재고 부족</option>
          <option value="RETURN_REVIEW">반품 검수 필요</option>
          <option value="STATUS_REVIEW">새 주문 상태 확인 필요</option>
        </Form.Select>
      )}
      {!!selectedIds.size && <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
        <Badge bg="primary">{selectedIds.size}건 선택</Badge>
        <Button size="sm" variant="outline-primary" disabled={bulkConfirm.isPending}
          onClick={() => bulkConfirm.mutate()}>
          {bulkConfirm.isPending ? "처리 중..." : "선택 주문 발주 확인"}
        </Button>
        <Button size="sm" variant="outline-success"
          onClick={() => setBulkDispatchOpen(true)}>선택 주문 발송</Button>
        <Button size="sm" variant="outline-secondary"
          onClick={() => setSelectedIds(new Set())}>선택 해제</Button>
      </div>}
      <ErrorAlert error={bulkConfirm.error} />
      {resolveReturn.error instanceof ApiError && resolveReturn.error.status === 409 && (
        <Alert variant="warning" role="alert">
          반품 검수 대상이 변경되었습니다. 목록에서 최신 수량을 다시 확인해 주세요.
        </Alert>
      )}
      {bulkResult && <BulkResultAlert result={bulkResult} />}
      {!orders.length ? (
        <EmptyState message={attentionOnly
          ? "확인이 필요한 스마트스토어 주문이 없습니다."
          : "수집된 스마트스토어 주문이 없습니다."} />
      ) : (
        <Table responsive hover size="sm" className="align-middle">
          <thead>
            <tr>
              <th style={{ width: 40 }}><Form.Check
                aria-label="전체 주문 선택"
                checked={orders.length > 0 && orders.slice(0, MAX_BULK_ORDERS)
                  .every((order) => selectedIds.has(order.productOrderId))}
                onChange={(event) => setSelectedIds(event.target.checked
                  ? new Set(orders.slice(0, MAX_BULK_ORDERS).map((order) => order.productOrderId))
                  : new Set())}
              /></th>
              <th>상품 주문 번호</th>
              <th>상품·옵션</th>
              <th>주문 상태</th>
              <th>수량</th>
              <th>내부 재고 반영</th>
              <th>확인 사유</th>
              <th>변경일</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.productOrderId}>
                <td><Form.Check
                  aria-label={`${order.productOrderId} 선택`}
                  checked={selectedIds.has(order.productOrderId)}
                  disabled={!selectedIds.has(order.productOrderId)
                    && selectedIds.size >= MAX_BULK_ORDERS}
                  onChange={(event) => setSelectedIds((current) => {
                    const next = new Set(current);
                    if (event.target.checked) next.add(order.productOrderId);
                    else next.delete(order.productOrderId);
                    return next;
                  })}
                /></td>
                <td className="small">{order.productOrderId}</td>
                <td>
                  <div>{order.productName}</div>
                  {order.productOption && (
                    <div className="small text-muted-soft">{order.productOption}</div>
                  )}
                </td>
                <td>{STATUS_LABELS[order.productOrderStatus] ?? order.productOrderStatus}</td>
                <td>
                  <div>{order.initialQuantity}개 / 잔여 {order.remainQuantity}개</div>
                  {order.pendingReturnQuantity > 0 && (
                    <div className="small text-warning-emphasis">미검수 반품 {order.pendingReturnQuantity}개</div>
                  )}
                </td>
                <td>{order.inventoryAppliedQuantity}개 차감</td>
                <td>
                  {order.attentionReason ? (
                    <Badge bg="warning" text="dark">
                      {ATTENTION_LABELS[order.attentionReason]}
                    </Badge>
                  ) : (
                    <Badge bg="success">정상</Badge>
                  )}
                </td>
                <td className="small">{formatDateTime(order.lastChangedAt)}</td>
                <td>{actions(
                  order,
                  pendingId,
                  retryInventory.mutate,
                  (order) => {
                    resolveReturn.reset();
                    setReturnReviewOrder(order);
                  },
                  setInventoryResolutionOrder,
                  setSelectedId,
                )}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
      {(pageCursors.length > 1 || data?.hasMore) && (
        <div className="d-flex justify-content-end gap-2 mb-3">
          <Button
            size="sm"
            variant="outline-secondary"
            disabled={pageCursors.length === 1}
            onClick={() => {
              setPageCursors((current) => current.slice(0, -1));
              setSelectedIds(new Set());
            }}
          >
            이전
          </Button>
          <Button
            size="sm"
            variant="outline-secondary"
            disabled={!data?.hasMore || !data.nextCursor}
            onClick={() => {
              if (data?.nextCursor) {
                setPageCursors((current) => [...current, data.nextCursor ?? undefined]);
                setSelectedIds(new Set());
              }
            }}
          >
            다음
          </Button>
        </div>
      )}
      <Modal
        show={returnReviewOrder !== null}
        onHide={() => { if (!resolveReturn.isPending) setReturnReviewOrder(null); }}
        centered
      >
        <Modal.Header closeButton={!resolveReturn.isPending}>
          <Modal.Title className="fs-6">스마트스토어 반품 검수</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={resolveReturn.error} />
          <div>{returnReviewOrder?.productName}</div>
          {returnReviewOrder?.productOption && <div>{returnReviewOrder.productOption}</div>}
          <div className="small text-muted-soft mb-3">상품 주문 번호: {returnReviewOrder?.productOrderId}</div>
          <p className="fw-semibold">이번 검수 대상: {returnReviewOrder?.pendingReturnQuantity}개</p>
          <p className="mb-0">표시된 수량을 모두 검수한 뒤 처리해 주세요. 판매 가능한 반품만 재고로 복원하세요.</p>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" disabled={resolveReturn.isPending}
            onClick={() => setReturnReviewOrder(null)}>닫기</Button>
          <Button variant="outline-secondary" disabled={resolveReturn.isPending}
            onClick={() => returnReviewOrder && resolveReturn.mutate({ order: returnReviewOrder, restoreStock: false })}>
            복원 없이 검수 완료
          </Button>
          <Button variant="success" disabled={resolveReturn.isPending}
            onClick={() => returnReviewOrder && resolveReturn.mutate({ order: returnReviewOrder, restoreStock: true })}>
            {returnReviewOrder?.pendingReturnQuantity}개 재고 복원
          </Button>
        </Modal.Footer>
      </Modal>
      {inventoryResolutionOrder && (
        <InventoryResolutionModal
          key={inventoryResolutionOrder.productOrderId}
          adminKey={adminKey}
          order={inventoryResolutionOrder}
          onAuthError={onAuthError}
          onClose={() => setInventoryResolutionOrder(null)}
          onCompleted={() => {
            setInventoryResolutionOrder(null);
            invalidate();
          }}
        />
      )}
      <SmartStoreOrderDetailModal
        adminKey={adminKey}
        productOrderId={selectedId}
        onAuthError={onAuthError}
        onClose={() => setSelectedId(null)}
        onChanged={invalidate}
      />
      <BulkDispatchModal
        show={bulkDispatchOpen}
        adminKey={adminKey}
        productOrderIds={[...selectedIds]}
        onAuthError={onAuthError}
        onClose={() => setBulkDispatchOpen(false)}
        onCompleted={(result) => {
          setBulkDispatchOpen(false);
          setBulkResult(result);
          setSelectedIds(new Set(result.failures.map((failure) => failure.productOrderId)));
          invalidate();
        }}
      />
      <DeliveryCompanyDatalist />
    </>
  );
}

function InventoryResolutionModal({
  adminKey,
  order,
  onAuthError,
  onClose,
  onCompleted,
}: {
  adminKey: string;
  order: SmartStoreChannelOrderResponse;
  onAuthError: () => void;
  onClose: () => void;
  onCompleted: () => void;
}) {
  const toast = useToast();
  const [productId, setProductId] = useState(order.productId ? String(order.productId) : "");
  const [variantId, setVariantId] = useState(
    order.productVariantId ? String(order.productVariantId) : "",
  );
  const [resolutionAction, setResolutionAction] =
    useState<ResolveSmartStoreInventoryRequestAction>(
      order.attentionReason === "STATUS_REVIEW" ? "KEEP_CURRENT" : "APPLY_REMAINING",
    );
  const [reason, setReason] = useState("");
  const productsQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });
  const selectedProduct = productsQuery.data?.find(
    (product) => product.id === Number(productId),
  );
  const submit = useAdminMutation(onAuthError, {
    mutationFn: () => resolveSmartStoreOrderInventory(
      adminKey,
      order.productOrderId,
      {
        productId: Number(productId),
        productVariantId: selectedProduct?.type === "MADE_TO_ORDER"
          ? Number(variantId)
          : null,
        action: resolutionAction,
        reason: reason.trim(),
        resolutionVersion: order.inventoryResolutionVersion,
      },
    ),
    onSuccess: (resolved) => {
      toast.show(resolved.attentionReason === "STOCK_SHORTAGE"
        ? "상품 연결은 저장했지만 재고가 부족합니다. 재고를 조정한 뒤 다시 시도해 주세요."
        : "스마트스토어 주문의 상품 연결과 재고 결정을 저장했습니다.");
      onCompleted();
    },
  });
  const valid = !!selectedProduct
    && (selectedProduct.type !== "MADE_TO_ORDER" || !!variantId)
    && !!reason.trim();

  return (
    <Modal show onHide={() => { if (!submit.isPending) onClose(); }} centered>
      <Form onSubmit={(event) => {
        event.preventDefault();
        if (valid) submit.mutate();
      }}>
        <Modal.Header closeButton={!submit.isPending}>
          <Modal.Title className="fs-6">스마트스토어 주문 수동 재고 결정</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={productsQuery.error ?? submit.error} />
          <div className="mb-3">
            <div className="fw-semibold">{order.productName}</div>
            {order.productOption && <div className="small text-muted-soft">{order.productOption}</div>}
            <div className="small text-muted-soft">상품 주문 번호: {order.productOrderId}</div>
          </div>
          {order.inventoryAppliedQuantity > 0 && (
            <Alert variant="warning" className="small">
              이미 {order.inventoryAppliedQuantity}개가 재고에 반영되어 있어 다른 상품이나 옵션 조합으로 바꿀 수 없습니다.
            </Alert>
          )}
          <Form.Group className="mb-3">
            <Form.Label>내부 상품</Form.Label>
            <Form.Select
              aria-label="내부 상품"
              required
              value={productId}
              disabled={productsQuery.isLoading}
              onChange={(event) => {
                setProductId(event.target.value);
                setVariantId("");
              }}
            >
              <option value="">상품 선택</option>
              {(productsQuery.data ?? []).map((product) => (
                <option key={product.id} value={product.id}>
                  {product.name} · {product.type === "READY_STOCK" ? "기성품" : "주문제작"}
                  {product.status === "INACTIVE" ? " · 판매 중지" : ""}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          {selectedProduct?.type === "MADE_TO_ORDER" && (
            <Form.Group className="mb-3">
              <Form.Label>옵션 조합</Form.Label>
              <Form.Select
                aria-label="옵션 조합"
                required
                value={variantId}
                onChange={(event) => setVariantId(event.target.value)}
              >
                <option value="">옵션 조합 선택</option>
                {selectedProduct.variants.map((variant) => (
                  <option key={variant.id} value={variant.id}>
                    {productVariantLabel(selectedProduct, variant)} · 재고 {variant.quantity}개
                    {!variant.active ? " · 사용 중지" : ""}
                  </option>
                ))}
              </Form.Select>
            </Form.Group>
          )}
          <Form.Group className="mb-3">
            <Form.Label>재고 결정</Form.Label>
            <Form.Select
              aria-label="재고 결정"
              value={resolutionAction}
              onChange={(event) => setResolutionAction(
                event.target.value as ResolveSmartStoreInventoryRequestAction,
              )}
            >
              <option value="APPLY_REMAINING">남은 판매 수량 {order.remainQuantity}개 차감</option>
              <option value="RESTORE_ALL">현재 차감 {order.inventoryAppliedQuantity}개 전부 복원</option>
              <option value="KEEP_CURRENT">현재 차감 {order.inventoryAppliedQuantity}개 유지</option>
            </Form.Select>
          </Form.Group>
          <Form.Group>
            <Form.Label>처리 사유</Form.Label>
            <Form.Control
              aria-label="처리 사유"
              as="textarea"
              rows={2}
              required
              maxLength={500}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              placeholder="상품 연결과 재고 결정을 확인한 근거를 입력하세요."
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" disabled={submit.isPending} onClick={onClose}>취소</Button>
          <Button type="submit" disabled={!valid || submit.isPending}>
            {submit.isPending ? "저장 중..." : "상품 연결과 재고 결정 저장"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}

function productVariantLabel(product: ProductResponse, variant: ProductVariantResponse): string {
  if (!variant.selections.length) return "기본 조합";
  return variant.selections.map((selection) => {
    const group = product.optionGroups.find((candidate) => candidate.key === selection.groupKey);
    const value = group?.values.find((candidate) => candidate.key === selection.valueKey);
    return `${group?.name ?? "옵션"}: ${value?.name ?? "값"}`;
  }).join(" / ");
}

function BulkResultAlert({ result }: { result: SmartStoreOrderBulkActionResponse }) {
  return <Alert variant={result.failures.length ? "warning" : "success"}>
    성공 {result.successProductOrderIds.length}건 · 실패 {result.failures.length}건
    {result.failures.length > 0 && <ul className="small mb-0 mt-2">
      {result.failures.map((failure) => <li key={failure.productOrderId}>
        {failure.productOrderId}: {failure.message}
        {failure.code ? ` (${failure.code})` : ""}
      </li>)}
    </ul>}
  </Alert>;
}

function BulkDispatchModal({
  show,
  adminKey,
  productOrderIds,
  onAuthError,
  onClose,
  onCompleted,
}: {
  show: boolean;
  adminKey: string;
  productOrderIds: string[];
  onAuthError: () => void;
  onClose: () => void;
  onCompleted: (result: SmartStoreOrderBulkActionResponse) => void;
}) {
  const toast = useToast();
  const [deliveryMethod, setDeliveryMethod] = useState("DELIVERY");
  const [deliveryCompanyCode, setDeliveryCompanyCode] = useState("");
  const [dispatchDate, setDispatchDate] = useState(currentLocalDateTime());
  const [trackingNumbers, setTrackingNumbers] = useState<Record<string, string>>({});
  const dispatch = useAdminMutation(onAuthError, {
    mutationFn: () => dispatchSmartStoreOrders(adminKey, {
      orders: productOrderIds.map((productOrderId) => ({
        productOrderId,
        deliveryMethod,
        deliveryCompanyCode: deliveryCompanyCode || undefined,
        trackingNumber: trackingNumbers[productOrderId] || undefined,
        dispatchDate,
      })),
    }),
    onSuccess: (result) => {
      toast.show(`발송 ${result.successProductOrderIds.length}건을 요청했습니다.`);
      onCompleted(result);
    },
  });
  const valid = productOrderIds.length > 0 && dispatchDate.length > 0;

  return <Modal show={show} onHide={onClose} size="lg" centered>
    <Form onSubmit={(event) => { event.preventDefault(); if (valid) dispatch.mutate(); }}>
      <Modal.Header closeButton><Modal.Title className="fs-6">
        스마트스토어 주문 {productOrderIds.length}건 발송
      </Modal.Title></Modal.Header>
      <Modal.Body>
        <ErrorAlert error={dispatch.error} />
        <Row className="g-2 mb-3">
          <Col md={4}><Form.Select value={deliveryMethod}
            onChange={(event) => setDeliveryMethod(event.target.value)}>
            <option value="DELIVERY">택배</option>
            <option value="DIRECT_DELIVERY">직접 배송</option>
            <option value="VISIT_RECEIPT">방문 수령</option>
            <option value="QUICK_SVC">퀵서비스</option>
          </Form.Select></Col>
          <Col md={4}><Form.Control list="smartstore-delivery-companies"
            value={deliveryCompanyCode}
            onChange={(event) => setDeliveryCompanyCode(event.target.value.toUpperCase())}
            placeholder="택배사 코드" /></Col>
          <Col md={4}><Form.Control type="datetime-local" required value={dispatchDate}
            onChange={(event) => setDispatchDate(event.target.value)} /></Col>
        </Row>
        <Table responsive size="sm" className="align-middle">
          <thead><tr><th>상품 주문 번호</th><th>운송장 번호</th></tr></thead>
          <tbody>{productOrderIds.map((productOrderId) => <tr key={productOrderId}>
            <td className="small">{productOrderId}</td>
            <td><Form.Control value={trackingNumbers[productOrderId] ?? ""}
              onChange={(event) => setTrackingNumbers((current) => ({
                ...current, [productOrderId]: event.target.value,
              }))} placeholder="운송장 번호" /></td>
          </tr>)}</tbody>
        </Table>
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose}>취소</Button>
        <Button type="submit" disabled={!valid || dispatch.isPending}>
          {dispatch.isPending ? "발송 요청 중..." : "일괄 발송"}
        </Button>
      </Modal.Footer>
    </Form>
  </Modal>;
}

function actions(
  order: SmartStoreChannelOrderResponse,
  pendingId: string | null,
  retry: (productOrderId: string) => void,
  openReturnReview: (order: SmartStoreChannelOrderResponse) => void,
  openInventoryResolution: (order: SmartStoreChannelOrderResponse) => void,
  openDetail: (productOrderId: string) => void,
) {
  const disabled = pendingId === order.productOrderId;
  if (order.attentionReason === "MAPPING_REQUIRED"
      || order.attentionReason === "STATUS_REVIEW") {
    return <div className="d-flex flex-wrap gap-1">
      <Button size="sm" variant="outline-secondary" onClick={() => openDetail(order.productOrderId)}>
        주문 처리
      </Button>
      <Button
          size="sm"
          variant="outline-primary"
          disabled={disabled}
          onClick={() => openInventoryResolution(order)}
        >
          상품 연결·재고 결정
        </Button>
    </div>;
  }
  if (order.attentionReason === "STOCK_SHORTAGE") {
    return <div className="d-flex flex-wrap gap-1">
      <Button size="sm" variant="outline-secondary" onClick={() => openDetail(order.productOrderId)}>
        주문 처리
      </Button>
      {order.productId && <LinkButton
        size="sm"
        variant="outline-warning"
        to={`/admin?view=products&productId=${order.productId}${order.productVariantId
          ? `&variantId=${order.productVariantId}` : ""}`}
      >
        재고 조정
      </LinkButton>}
      <Button size="sm" variant="outline-primary" disabled={disabled}
        onClick={() => retry(order.productOrderId)}>
        재고 반영 다시 시도
      </Button>
    </div>;
  }
  if (order.attentionReason !== "RETURN_REVIEW") {
    return <Button
      size="sm"
      variant="outline-secondary"
      onClick={() => openDetail(order.productOrderId)}
    >
      주문 처리
    </Button>;
  }
  return (
    <div className="d-flex flex-wrap gap-1">
      <Button
        size="sm"
        variant="outline-secondary"
        onClick={() => openDetail(order.productOrderId)}
      >
        주문 처리
      </Button>
      <Button
        size="sm"
        variant="outline-success"
        disabled={disabled}
        onClick={() => openReturnReview(order)}
      >
        반품 검수
      </Button>
    </div>
  );
}

type ChannelAction =
  | { kind: "confirm" }
  | { kind: "dispatch"; request: DispatchSmartStoreOrderRequest }
  | { kind: "delay"; request: DelaySmartStoreOrderRequest }
  | { kind: "approveCancel" }
  | { kind: "approveReturn" }
  | { kind: "rejectReturn" }
  | { kind: "dispatchExchange"; request: DispatchSmartStoreExchangeRequest }
  | { kind: "completeExchangeCollect" }
  | { kind: "rejectExchange"; reason: string }
  | { kind: "holdExchange"; request: HoldSmartStoreExchangeRequest }
  | { kind: "releaseExchangeHold" }
  | { kind: "holdReturn"; request: HoldSmartStoreReturnRequest }
  | { kind: "releaseReturnHold" }
  | { kind: "requestSellerReturn"; request: RequestSmartStoreSellerReturnRequest }
  | { kind: "requestSellerCancel"; request: RequestSmartStoreSellerCancelRequest };

function SmartStoreOrderDetailModal({
  adminKey,
  productOrderId,
  onAuthError,
  onClose,
  onChanged,
}: {
  adminKey: string;
  productOrderId: string | null;
  onAuthError: () => void;
  onClose: () => void;
  onChanged: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [deliveryMethod, setDeliveryMethod] = useState("DELIVERY");
  const [deliveryCompanyCode, setDeliveryCompanyCode] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");
  const [dispatchDate, setDispatchDate] = useState("");
  const [dispatchDueDate, setDispatchDueDate] = useState("");
  const [delayReasonCode, setDelayReasonCode] = useState("PRODUCT_PREPARE");
  const [delayDetail, setDelayDetail] = useState("");
  const [exchangeRejectReason, setExchangeRejectReason] = useState("");
  const [holdbackClassType, setHoldbackClassType] = useState("EXCHANGE_DELIVERYFEE");
  const [holdbackReason, setHoldbackReason] = useState("");
  const [extraExchangeFeeAmount, setExtraExchangeFeeAmount] = useState("");
  const [cancelReason, setCancelReason] = useState("SOLD_OUT");
  const [cancelDetailedReason, setCancelDetailedReason] = useState("");
  const [cancelQuantity, setCancelQuantity] = useState("");
  const [returnHoldbackClassType, setReturnHoldbackClassType] = useState("RETURN_DELIVERYFEE");
  const [returnHoldbackReason, setReturnHoldbackReason] = useState("");
  const [extraReturnFeeAmount, setExtraReturnFeeAmount] = useState("");
  const [sellerReturnReason, setSellerReturnReason] = useState("PRODUCT_UNSATISFIED");
  const [collectDeliveryMethod, setCollectDeliveryMethod] = useState("RETURN_DESIGNATED");
  const [collectDeliveryCompany, setCollectDeliveryCompany] = useState("");
  const [collectTrackingNumber, setCollectTrackingNumber] = useState("");
  const [returnQuantity, setReturnQuantity] = useState("");

  const detailQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-orders", "detail", productOrderId],
    queryFn: () => fetchSmartStoreChannelOrder(adminKey, productOrderId!),
    enabled: productOrderId !== null,
  });
  const historyQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-orders", "actions", productOrderId],
    queryFn: () => fetchSmartStoreChannelOrderActions(adminKey, productOrderId!),
    enabled: productOrderId !== null,
  });

  const action = useAdminMutation(onAuthError, {
    mutationFn: async (request: ChannelAction) => {
      if (!productOrderId) return;
      if (request.kind === "confirm") {
        await confirmSmartStoreOrder(adminKey, productOrderId);
      } else if (request.kind === "dispatch") {
        await dispatchSmartStoreOrder(adminKey, productOrderId, request.request);
      } else if (request.kind === "delay") {
        await delaySmartStoreOrder(adminKey, productOrderId, request.request);
      } else if (request.kind === "approveCancel") {
        await approveSmartStoreCancel(adminKey, productOrderId);
      } else if (request.kind === "approveReturn") {
        await approveSmartStoreReturn(adminKey, productOrderId);
      } else if (request.kind === "rejectReturn") {
        await rejectSmartStoreReturn(adminKey, productOrderId);
      } else if (request.kind === "dispatchExchange") {
        await dispatchSmartStoreExchange(adminKey, productOrderId, request.request);
      } else if (request.kind === "completeExchangeCollect") {
        await completeSmartStoreExchangeCollection(adminKey, productOrderId);
      } else if (request.kind === "rejectExchange") {
        await rejectSmartStoreExchange(adminKey, productOrderId, request.reason);
      } else if (request.kind === "holdExchange") {
        await holdSmartStoreExchange(adminKey, productOrderId, request.request);
      } else if (request.kind === "releaseExchangeHold") {
        await releaseSmartStoreExchange(adminKey, productOrderId);
      } else if (request.kind === "holdReturn") {
        await holdSmartStoreReturn(adminKey, productOrderId, request.request);
      } else if (request.kind === "releaseReturnHold") {
        await releaseSmartStoreReturn(adminKey, productOrderId);
      } else if (request.kind === "requestSellerReturn") {
        await requestSmartStoreOrderReturn(adminKey, productOrderId, request.request);
      } else if (request.kind === "requestSellerCancel") {
        await requestSmartStoreOrderCancel(adminKey, productOrderId, request.request);
      }
    },
    onSuccess: async () => {
      toast.show("스마트스토어에 주문 처리를 요청했습니다. 변경 상태는 주문 동기화 후 반영됩니다.");
      onChanged();
      await queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-orders", "detail", productOrderId],
      });
      await queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-orders", "actions", productOrderId],
      });
    },
  });

  const detail = detailQuery.data;
  const order = detail?.order;
  const dispatchRequest = {
    deliveryMethod,
    deliveryCompanyCode: deliveryCompanyCode || undefined,
    trackingNumber: trackingNumber || undefined,
    dispatchDate,
  } satisfies DispatchSmartStoreOrderRequest;
  const exchangeRequest = {
    deliveryMethod,
    deliveryCompanyCode,
    trackingNumber,
  } satisfies DispatchSmartStoreExchangeRequest;
  const canPlaceOrder = order?.productOrderStatus === "PAYED";
  const claimStatus = order?.claimStatus;

  return (
    <Modal show={productOrderId !== null} onHide={onClose} size="lg" centered>
      <Modal.Header closeButton>
        <Modal.Title>스마트스토어 주문 처리</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {detailQuery.isLoading && <LoadingSpinner />}
        <ErrorAlert error={detailQuery.error ?? historyQuery.error ?? action.error} />
        {detail && order && (
          <>
            <div className="mb-3">
              <div className="fw-semibold">{order.productName}</div>
              <div className="small text-muted-soft">
                상품 주문 {order.productOrderId} · {STATUS_LABELS[order.productOrderStatus] ?? order.productOrderStatus}
                {order.productOption ? ` · ${order.productOption}` : ""}
              </div>
            </div>

            <Row className="g-3 mb-3 small">
              <Col md={6}>
                <div className="fw-semibold mb-1">수령인·배송지</div>
                {detail.deliveryInfo ? (
                  <div>
                    <div>{detail.deliveryInfo.recipientName} · {detail.deliveryInfo.phone}</div>
                    <div>[{detail.deliveryInfo.postalCode}] {detail.deliveryInfo.addressLine1} {detail.deliveryInfo.addressLine2}</div>
                    {detail.deliveryInfo.shippingMemo && <div>배송 메모: {detail.deliveryInfo.shippingMemo}</div>}
                  </div>
                ) : <div className="text-muted-soft">배송지 정보 없음</div>}
              </Col>
              <Col md={6}>
                <div className="fw-semibold mb-1">결제·정산</div>
                <div>결제 금액: {amount(detail.paymentAmount)}</div>
                <div>정산 예정: {amount(detail.expectedSettlementAmount)}</div>
                <div>결제 수수료: {amount(detail.paymentCommission)} · 판매 수수료: {amount(detail.saleCommission)}</div>
              </Col>
              <Col md={6}>
                <div className="fw-semibold mb-1">발주·배송</div>
                <div>발주 상태: {detail.placeOrderStatus ?? "-"}</div>
                <div>발송 기한: {detail.shippingDueDate ? formatDateTime(detail.shippingDueDate) : "-"}</div>
                <div>택배사·운송장: {detail.deliveryCompany ?? "-"} {detail.trackingNumber ?? ""}</div>
              </Col>
              <Col md={6}>
                <div className="fw-semibold mb-1">클레임</div>
                <div>{order.claimType ?? "없음"} · {claimStatus ?? "-"}</div>
                {detail.claimDetail && <div className="mt-1">
                  <div>사유: {detail.claimDetail.reason ?? "-"}</div>
                  {detail.claimDetail.detailedReason && <div>{detail.claimDetail.detailedReason}</div>}
                  <div>요청 수량: {detail.claimDetail.requestQuantity ?? "-"}개</div>
                  <div>요청일: {detail.claimDetail.requestedAt
                    ? formatDateTime(detail.claimDetail.requestedAt) : "-"}</div>
                  {detail.claimDetail.collectStatus && <div>
                    수거: {detail.claimDetail.collectStatus}
                    {detail.claimDetail.collectTrackingNumber
                      ? ` · ${detail.claimDetail.collectDeliveryCompany ?? ""} ${detail.claimDetail.collectTrackingNumber}` : ""}
                  </div>}
                  {detail.claimDetail.claimDeliveryFeeDemandAmount !== null && <div>
                    클레임 배송비: {formatKRW(detail.claimDetail.claimDeliveryFeeDemandAmount)}
                  </div>}
                  {detail.claimDetail.holdbackStatus && <div>보류 상태: {detail.claimDetail.holdbackStatus}</div>}
                  {!!detail.claimDetail.imageUrls.length && <div className="d-flex flex-wrap gap-2 mt-2">
                    {detail.claimDetail.imageUrls.map((imageUrl) => <a key={imageUrl}
                      href={imageUrl} target="_blank" rel="noreferrer">
                      <img src={imageUrl} alt="클레임 첨부" width={72} height={72}
                        className="rounded border object-fit-cover" />
                    </a>)}
                  </div>}
                </div>}
              </Col>
            </Row>

            <SmartStoreOrderActionHistory
              history={historyQuery.data ?? []}
              loading={historyQuery.isLoading}
            />

            {canPlaceOrder && detail.placeOrderStatus === "NOT_YET" && (
              <Button
                className="mb-3"
                disabled={action.isPending}
                onClick={() => action.mutate({ kind: "confirm" })}
              >
                발주 확인
              </Button>
            )}

            {canPlaceOrder && detail.placeOrderStatus === "OK" && (
              <Row className="g-3 mb-3">
                <Col md={6}>
                  <Form
                    onSubmit={(event) => {
                      event.preventDefault();
                      action.mutate({ kind: "dispatch", request: dispatchRequest });
                    }}
                  >
                    <div className="fw-semibold mb-2">발송 처리</div>
                    <DeliveryFields
                      deliveryMethod={deliveryMethod}
                      deliveryCompanyCode={deliveryCompanyCode}
                      trackingNumber={trackingNumber}
                      onDeliveryMethod={setDeliveryMethod}
                      onDeliveryCompanyCode={setDeliveryCompanyCode}
                      onTrackingNumber={setTrackingNumber}
                    />
                    <Form.Control
                      className="mb-2"
                      type="datetime-local"
                      required
                      value={dispatchDate}
                      onChange={(event) => setDispatchDate(event.target.value)}
                      aria-label="발송일시"
                    />
                    <Button type="submit" size="sm" disabled={action.isPending}>발송 처리</Button>
                  </Form>
                </Col>
                <Col md={6}>
                  <Form
                    onSubmit={(event) => {
                      event.preventDefault();
                      action.mutate({
                        kind: "delay",
                        request: {
                          dispatchDueDate,
                          reasonCode: delayReasonCode,
                          detailedReason: delayDetail,
                        },
                      });
                    }}
                  >
                    <div className="fw-semibold mb-2">발송 지연</div>
                    <Form.Control
                      className="mb-2"
                      type="datetime-local"
                      required
                      value={dispatchDueDate}
                      onChange={(event) => setDispatchDueDate(event.target.value)}
                      aria-label="변경 발송 기한"
                    />
                    <Form.Select
                      className="mb-2"
                      value={delayReasonCode}
                      onChange={(event) => setDelayReasonCode(event.target.value)}
                      aria-label="발송 지연 사유"
                    >
                      <option value="PRODUCT_PREPARE">상품 준비</option>
                      <option value="CUSTOMER_REQUEST">고객 요청</option>
                      <option value="CUSTOM_BUILD">주문 제작</option>
                      <option value="RESERVED_DISPATCH">예약 발송</option>
                      <option value="OVERSEA_DELIVERY">해외 배송</option>
                      <option value="ETC">기타</option>
                    </Form.Select>
                    <Form.Control
                      className="mb-2"
                      as="textarea"
                      rows={2}
                      maxLength={4000}
                      value={delayDetail}
                      onChange={(event) => setDelayDetail(event.target.value)}
                      placeholder="상세 사유"
                    />
                    <Button type="submit" size="sm" variant="outline-warning" disabled={action.isPending}>
                      발송 기한 변경
                    </Button>
                  </Form>
                </Col>
              </Row>
            )}

            <div className="d-flex flex-wrap gap-2 mb-3">
              {claimStatus === "CANCEL_REQUEST" && (
                <Button variant="danger" size="sm" disabled={action.isPending}
                  onClick={() => action.mutate({ kind: "approveCancel" })}>
                  취소 승인
                </Button>
              )}
              {order.claimType === "RETURN" && ["RETURN_REQUEST", "COLLECT_DONE"].includes(claimStatus ?? "") && (
                <Button variant="danger" size="sm" disabled={action.isPending}
                  onClick={() => action.mutate({ kind: "approveReturn" })}>
                  반품 승인
                </Button>
              )}
              {order.claimType === "RETURN" && ["RETURN_REQUEST", "COLLECTING"].includes(claimStatus ?? "") && (
                <Button variant="outline-secondary" size="sm" disabled={action.isPending}
                  onClick={() => action.mutate({ kind: "rejectReturn" })}>
                  반품 거부
                </Button>
              )}
            </div>

            {order.claimType === "RETURN" && (
              <div className="border rounded p-3 mb-3">
                <div className="d-flex justify-content-between align-items-center mb-2">
                  <div className="fw-semibold">반품 보류</div>
                  {detail.claimDetail?.holdbackStatus && (
                    <Button type="button" size="sm" variant="outline-primary"
                      disabled={action.isPending}
                      onClick={() => action.mutate({ kind: "releaseReturnHold" })}>
                      보류 해제
                    </Button>
                  )}
                </div>
                <Form onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({
                    kind: "holdReturn",
                    request: {
                      holdbackClassType: returnHoldbackClassType,
                      detailedReason: returnHoldbackReason,
                      extraReturnFeeAmount: extraReturnFeeAmount
                        ? Number(extraReturnFeeAmount) : undefined,
                    },
                  });
                }}>
                  <Row className="g-2">
                    <Col md={4}><Form.Select value={returnHoldbackClassType}
                      onChange={(event) => setReturnHoldbackClassType(event.target.value)}>
                      <option value="RETURN_DELIVERYFEE">반품 배송비</option>
                      <option value="RETURN_EXTRAFEE">추가 비용</option>
                      <option value="PURCHASER_CONFIRM_NEED">구매자 확인 필요</option>
                      <option value="SELLER_CONFIRM_NEED">판매자 확인 필요</option>
                      <option value="ETC">기타</option>
                    </Form.Select></Col>
                    <Col md={5}><Form.Control required value={returnHoldbackReason}
                      onChange={(event) => setReturnHoldbackReason(event.target.value)}
                      placeholder="보류 상세 사유" /></Col>
                    <Col md={3}><Form.Control type="number" min={0} value={extraReturnFeeAmount}
                      onChange={(event) => setExtraReturnFeeAmount(event.target.value)}
                      placeholder="추가 비용" /></Col>
                  </Row>
                  <Button className="mt-2" type="submit" size="sm" variant="outline-warning"
                    disabled={action.isPending || !returnHoldbackReason.trim()}>반품 보류</Button>
                </Form>
              </div>
            )}

            {!order.claimType && ["PAYED", "DELIVERING"].includes(order.productOrderStatus) && (
              <Form className="border rounded p-3 mb-3" onSubmit={(event) => {
                event.preventDefault();
                action.mutate({
                  kind: "requestSellerCancel",
                  request: {
                    reason: cancelReason,
                    detailedReason: cancelDetailedReason || undefined,
                    quantity: cancelQuantity ? Number(cancelQuantity) : undefined,
                  },
                });
              }}>
                <div className="fw-semibold mb-2">판매자 취소 요청</div>
                <Row className="g-2">
                  <Col md={4}><Form.Select value={cancelReason}
                    onChange={(event) => setCancelReason(event.target.value)}>
                    <option value="SOLD_OUT">품절</option>
                    <option value="DELAYED_DELIVERY">배송 지연</option>
                    <option value="INCORRECT_INFO">상품 정보 오류</option>
                    <option value="PRODUCT_UNSATISFIED">상품 문제</option>
                  </Form.Select></Col>
                  <Col md={2}><Form.Control type="number" min={1} max={order.remainQuantity}
                    value={cancelQuantity} onChange={(event) => setCancelQuantity(event.target.value)}
                    placeholder="수량(전체)" /></Col>
                  <Col md={6}><Form.Control maxLength={500} value={cancelDetailedReason}
                    onChange={(event) => setCancelDetailedReason(event.target.value)}
                    placeholder="상세 사유" /></Col>
                </Row>
                <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
                  disabled={action.isPending}>취소 요청</Button>
              </Form>
            )}

            {!order.claimType && (
              <Form className="border rounded p-3 mb-3" onSubmit={(event) => {
                event.preventDefault();
                action.mutate({
                  kind: "requestSellerReturn",
                  request: {
                    returnReason: sellerReturnReason,
                    collectDeliveryMethod,
                    collectDeliveryCompany: collectDeliveryCompany || undefined,
                    collectTrackingNumber: collectTrackingNumber || undefined,
                    returnQuantity: returnQuantity ? Number(returnQuantity) : undefined,
                  },
                });
              }}>
                <div className="fw-semibold mb-2">판매자 반품 요청</div>
                <Row className="g-2">
                  <Col md={4}><Form.Select value={sellerReturnReason}
                    onChange={(event) => setSellerReturnReason(event.target.value)}>
                    <option value="PRODUCT_UNSATISFIED">상품 문제</option>
                    <option value="DELAYED_DELIVERY">배송 지연</option>
                    <option value="SOLD_OUT">품절</option>
                    <option value="WRONG_ORDER">오배송</option>
                    <option value="BROKEN">상품 파손</option>
                  </Form.Select></Col>
                  <Col md={4}><Form.Select value={collectDeliveryMethod}
                    onChange={(event) => setCollectDeliveryMethod(event.target.value)}>
                    <option value="RETURN_DESIGNATED">지정 택배 수거</option>
                    <option value="RETURN_INDIVIDUAL">구매자 직접 반송</option>
                  </Form.Select></Col>
                  <Col md={4}><Form.Control type="number" min={1} max={order.remainQuantity}
                    value={returnQuantity} onChange={(event) => setReturnQuantity(event.target.value)}
                    placeholder="수량(전체)" /></Col>
                  <Col md={6}><Form.Control list="smartstore-delivery-companies"
                    value={collectDeliveryCompany}
                    onChange={(event) => setCollectDeliveryCompany(event.target.value.toUpperCase())}
                    placeholder="수거 택배사 코드 (선택)" /></Col>
                  <Col md={6}><Form.Control value={collectTrackingNumber}
                    onChange={(event) => setCollectTrackingNumber(event.target.value)}
                    placeholder="수거 운송장 번호 (선택)" /></Col>
                </Row>
                <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
                  disabled={action.isPending}>반품 요청</Button>
              </Form>
            )}

            {order.claimType === "EXCHANGE" && (
              <div className="border rounded p-3 mb-3">
                <div className="fw-semibold mb-2">교환 클레임 처리</div>
                <div className="d-flex flex-wrap gap-2 mb-3">
                  <Button size="sm" variant="outline-success" disabled={action.isPending}
                    onClick={() => action.mutate({ kind: "completeExchangeCollect" })}>
                    수거 완료
                  </Button>
                  {detail.claimDetail?.holdbackStatus && <Button size="sm" variant="outline-primary"
                    disabled={action.isPending}
                    onClick={() => action.mutate({ kind: "releaseExchangeHold" })}>
                    보류 해제
                  </Button>}
                </div>
                <Form className="mb-3" onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({ kind: "rejectExchange", reason: exchangeRejectReason });
                }}>
                  <Form.Control required maxLength={500} value={exchangeRejectReason}
                    onChange={(event) => setExchangeRejectReason(event.target.value)}
                    placeholder="교환 거절 사유" />
                  <Button className="mt-2" type="submit" size="sm" variant="outline-danger"
                    disabled={action.isPending || !exchangeRejectReason.trim()}>교환 거절</Button>
                </Form>
                <Form onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({
                    kind: "holdExchange",
                    request: {
                      holdbackClassType,
                      detailedReason: holdbackReason,
                      extraExchangeFeeAmount: extraExchangeFeeAmount
                        ? Number(extraExchangeFeeAmount) : undefined,
                    },
                  });
                }}>
                  <Row className="g-2">
                    <Col md={4}><Form.Select value={holdbackClassType}
                      onChange={(event) => setHoldbackClassType(event.target.value)}>
                      <option value="EXCHANGE_DELIVERYFEE">교환 배송비</option>
                      <option value="EXCHANGE_EXTRAFEE">추가 비용</option>
                      <option value="EXCHANGE_PRODUCT_READY">교환 상품 준비</option>
                      <option value="PURCHASER_CONFIRM_NEED">구매자 확인 필요</option>
                      <option value="SELLER_CONFIRM_NEED">판매자 확인 필요</option>
                      <option value="ETC">기타</option>
                    </Form.Select></Col>
                    <Col md={5}><Form.Control required maxLength={500} value={holdbackReason}
                      onChange={(event) => setHoldbackReason(event.target.value)}
                      placeholder="보류 상세 사유" /></Col>
                    <Col md={3}><Form.Control type="number" min={0} value={extraExchangeFeeAmount}
                      onChange={(event) => setExtraExchangeFeeAmount(event.target.value)}
                      placeholder="추가 비용" /></Col>
                  </Row>
                  <Button className="mt-2" type="submit" size="sm" variant="outline-warning"
                    disabled={action.isPending || !holdbackReason.trim()}>교환 보류</Button>
                </Form>
              </div>
            )}

            {order.claimType === "EXCHANGE" && claimStatus === "COLLECT_DONE" && (
              <Form
                onSubmit={(event) => {
                  event.preventDefault();
                  action.mutate({ kind: "dispatchExchange", request: exchangeRequest });
                }}
              >
                <div className="fw-semibold mb-2">교환품 재배송</div>
                <DeliveryFields
                  required
                  deliveryMethod={deliveryMethod}
                  deliveryCompanyCode={deliveryCompanyCode}
                  trackingNumber={trackingNumber}
                  onDeliveryMethod={setDeliveryMethod}
                  onDeliveryCompanyCode={setDeliveryCompanyCode}
                  onTrackingNumber={setTrackingNumber}
                />
                <Button type="submit" size="sm" disabled={action.isPending}>교환품 발송</Button>
              </Form>
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={onClose}>닫기</Button>
      </Modal.Footer>
    </Modal>
  );
}

function SmartStoreOrderActionHistory({
  history,
  loading,
}: {
  history: SmartStoreOrderActionHistoryResponse[];
  loading: boolean;
}) {
  return (
    <section className="border rounded p-3 mb-3" aria-label="주문 처리 이력">
      <div className="fw-semibold mb-2">최근 처리 이력</div>
      {loading ? (
        <div className="small text-muted-soft">처리 이력을 불러오는 중입니다.</div>
      ) : !history.length ? (
        <div className="small text-muted-soft">저장된 처리 이력이 없습니다.</div>
      ) : (
        <div className="d-grid gap-2">
          {history.map((item) => (
            <div key={item.id} className="small border-bottom pb-2">
              <div className="d-flex flex-wrap justify-content-between gap-2">
                <span className="fw-semibold">{ACTION_LABELS[item.action] ?? item.action}</span>
                <Badge bg={actionStatusColor(item.status)}>
                  {ACTION_STATUS_LABELS[item.status] ?? item.status}
                </Badge>
              </div>
              <div className="text-muted-soft">
                {item.changedBy} · {formatDateTime(item.requestedAt)}
              </div>
              {item.requestSummary && <div>{item.requestSummary}</div>}
              {item.resultMessage && (
                <div className={item.status === "SUCCEEDED" ? "" : "text-danger"}>
                  {item.resultMessage}{item.resultCode ? ` (${item.resultCode})` : ""}
                </div>
              )}
              {item.reconciliationOutcome && (
                <div className="mt-1 text-primary-emphasis">
                  대사 결과: {item.reconciliationOutcome === "APPLIED"
                    ? "네이버에 반영됨"
                    : "네이버에 반영되지 않음"}
                  {item.reconciledBy && ` · ${item.reconciledBy}`}
                  {item.reconciledAt && ` · ${formatDateTime(item.reconciledAt)}`}
                  {item.reconciliationNote && <div>{item.reconciliationNote}</div>}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function actionStatusColor(status: SmartStoreOrderActionHistoryResponse["status"]): string {
  if (status === "SUCCEEDED") return "success";
  if (status === "REJECTED") return "danger";
  if (status === "NOT_SENT") return "secondary";
  if (status === "RESULT_UNKNOWN") return "warning";
  return "secondary";
}

function DeliveryFields({
  required = false,
  deliveryMethod,
  deliveryCompanyCode,
  trackingNumber,
  onDeliveryMethod,
  onDeliveryCompanyCode,
  onTrackingNumber,
}: {
  required?: boolean;
  deliveryMethod: string;
  deliveryCompanyCode: string;
  trackingNumber: string;
  onDeliveryMethod: (value: string) => void;
  onDeliveryCompanyCode: (value: string) => void;
  onTrackingNumber: (value: string) => void;
}) {
  return <>
    <Form.Select
      className="mb-2"
      value={deliveryMethod}
      onChange={(event) => onDeliveryMethod(event.target.value)}
      aria-label="배송 수단"
    >
      <option value="DELIVERY">택배</option>
      <option value="DIRECT_DELIVERY">직접 배송</option>
      <option value="VISIT_RECEIPT">방문 수령</option>
      <option value="QUICK_SVC">퀵서비스</option>
    </Form.Select>
    <Form.Control
      className="mb-2"
      list="smartstore-delivery-companies"
      required={required}
      value={deliveryCompanyCode}
      maxLength={40}
      onChange={(event) => onDeliveryCompanyCode(event.target.value.toUpperCase())}
      placeholder="택배사 코드 (예: CJGLS)"
    />
    <Form.Control
      className="mb-2"
      required={required}
      value={trackingNumber}
      maxLength={100}
      onChange={(event) => onTrackingNumber(event.target.value)}
      placeholder="운송장 번호"
    />
  </>;
}

function DeliveryCompanyDatalist() {
  return <datalist id="smartstore-delivery-companies">
    <option value="CJGLS">CJ대한통운</option>
    <option value="HYUNDAI">롯데택배</option>
    <option value="HANJIN">한진택배</option>
    <option value="KGB">로젠택배</option>
    <option value="EPOST">우체국택배</option>
    <option value="CVSNET">GS25 편의점택배</option>
    <option value="CUPARCEL">CU 편의점택배</option>
    <option value="KDEXP">경동택배</option>
    <option value="DAESIN">대신택배</option>
  </datalist>;
}

function currentLocalDateTime(): string {
  const date = new Date();
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function amount(value: number | null): string {
  return value === null ? "-" : formatKRW(value);
}
