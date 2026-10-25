import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Form, Table } from "react-bootstrap";
import type {
  ListSmartStoreChannelOrdersAttentionReason,
  SmartStoreChannelOrderResponse,
  SmartStoreOrderBulkActionResponse,
} from "@/generated/api/adminOrder";
import { ApiError } from "@/shared/api";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { ConfirmModal } from "@/shared/ui/ConfirmModal";
import { CursorPager } from "@/shared/ui/CursorPager";
import {
  confirmSmartStoreOrders,
  fetchSmartStoreChannelOrders,
  resolveSmartStoreReturn,
  retrySmartStoreOrderInventory,
} from "./api";
import { BulkDispatchModal, BulkResultAlert } from "./BulkDispatchModal";
import { InventoryResolutionModal } from "./InventoryResolutionModal";
import { ATTENTION_LABELS, STATUS_LABELS } from "./labels";
import { OrderRowActions } from "./OrderRowActions";
import { ReturnDeliveryCompanies } from "./ReturnDeliveryCompanies";
import { SmartStoreOrderDetailModal } from "./SmartStoreOrderDetailModal";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  attentionOnly?: boolean;
  focusProductOrderId?: string;
}

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
  const { cursor, hasPreviousPage, showNextPage, showPreviousPage, resetCursor } = useCursorHistory();
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(focusProductOrderId ?? null);
  const [inventoryResolutionOrder, setInventoryResolutionOrder] =
    useState<SmartStoreChannelOrderResponse | null>(null);
  const [returnReviewOrder, setReturnReviewOrder] = useState<SmartStoreChannelOrderResponse | null>(null);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkDispatchIds, setBulkDispatchIds] = useState<string[] | null>(null);
  const [bulkResult, setBulkResult] = useState<SmartStoreOrderBulkActionResponse | null>(null);
  const queryKey = [
    "admin", "smartstore-orders", attentionOnly, attentionReason || null, cursor ?? null,
  ] as const;
  const { data, isLoading, error, refetch, isFetching } = useAdminQuery(onAuthError, {
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
    resetCursor();
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
        ? "재고를 반영하지 못했습니다. 목록의 확인 필요 사유를 확인해 주세요."
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
        ? "반품 검수 수량을 해피갤러리 재고에 복원했습니다."
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
    mutationFn: (productOrderIds: string[]) => confirmSmartStoreOrders(adminKey, productOrderIds),
    onSuccess: (result) => {
      setBulkResult(result);
      setSelectedIds(new Set(result.failures.map((failure) => failure.productOrderId)));
      toast.show(`발주 확인 ${result.successProductOrderIds.length}건을 요청했습니다.`);
      invalidate();
    },
  });
  const selectionLocked = bulkConfirm.isPending || bulkDispatchIds !== null;

  if (isLoading) return <LoadingSpinner />;
  if (error && !data) {
    if (isAdminSessionUnauthorized(error)) return null;
    return <ErrorAlert error={error} onRetry={() => { void refetch(); }} retrying={isFetching} />;
  }

  return (
    <>
      <ErrorAlert error={error} onRetry={() => { void refetch(); }} retrying={isFetching} />
      {!initialAttentionOnly && <ReturnDeliveryCompanies adminKey={adminKey} onAuthError={onAuthError} />}
      {!initialAttentionOnly && (
        <Form.Check
          className="mb-3"
          type="switch"
          id="smartstore-order-attention-only"
          label="확인이 필요한 주문만 보기"
          checked={attentionOnly}
          disabled={selectionLocked}
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
          aria-label="확인 필요 사유 필터"
          value={attentionReason}
          disabled={selectionLocked}
          onChange={(event) => {
            setAttentionReason(event.target.value as ListSmartStoreChannelOrdersAttentionReason | "");
            resetPages();
          }}
        >
          <option value="">전체 사유</option>
          {Object.entries(ATTENTION_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </Form.Select>
      )}
      {!!selectedIds.size && <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
        <Badge bg="primary">{selectedIds.size}건 선택</Badge>
        <Button size="sm" variant="outline-primary" disabled={selectionLocked}
          onClick={() => bulkConfirm.mutate([...selectedIds])}>
          {bulkConfirm.isPending ? "처리 중..." : "선택 주문 발주 확인"}
        </Button>
        <Button size="sm" variant="outline-success" disabled={selectionLocked}
          onClick={() => setBulkDispatchIds([...selectedIds])}>선택 주문 발송</Button>
        <Button size="sm" variant="outline-secondary" disabled={selectionLocked}
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
                disabled={selectionLocked}
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
              <th>해피갤러리 재고 반영</th>
              <th>확인 필요 사유</th>
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
                  disabled={selectionLocked || (!selectedIds.has(order.productOrderId)
                    && selectedIds.size >= MAX_BULK_ORDERS)}
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
                <td><fieldset disabled={selectionLocked}>
                  <OrderRowActions
                    order={order}
                    pendingId={pendingId}
                    onRetry={retryInventory.mutate}
                    onReturnReview={(order) => {
                      resolveReturn.reset();
                      setReturnReviewOrder(order);
                    }}
                    onInventoryResolution={setInventoryResolutionOrder}
                    onOpenDetail={setSelectedId}
                  />
                </fieldset></td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
      <CursorPager
        className="d-flex justify-content-end gap-2 mb-3"
        nextVariant="outline-secondary"
        hasPreviousPage={hasPreviousPage}
        page={data}
        busy={selectionLocked}
        onPrevious={() => {
          showPreviousPage();
          setSelectedIds(new Set());
        }}
        onNext={(nextCursor) => {
          showNextPage(nextCursor);
          setSelectedIds(new Set());
        }}
      />
      <ConfirmModal
        show={returnReviewOrder !== null}
        title="스마트스토어 반품 검수"
        pending={resolveReturn.isPending}
        onClose={() => setReturnReviewOrder(null)}
        cancelVariant="secondary"
        extraActions={(
          <Button variant="outline-secondary" disabled={resolveReturn.isPending}
            onClick={() => returnReviewOrder && resolveReturn.mutate({ order: returnReviewOrder, restoreStock: false })}>
            복원 없이 검수 완료
          </Button>
        )}
        confirmLabel={<>{returnReviewOrder?.pendingReturnQuantity}개 재고 복원</>}
        confirmVariant="success"
        onConfirm={() => returnReviewOrder && resolveReturn.mutate({ order: returnReviewOrder, restoreStock: true })}
      >
        <ErrorAlert error={resolveReturn.error} />
        <div>{returnReviewOrder?.productName}</div>
        {returnReviewOrder?.productOption && <div>{returnReviewOrder.productOption}</div>}
        <div className="small text-muted-soft mb-3">상품 주문 번호: {returnReviewOrder?.productOrderId}</div>
        <p className="fw-semibold">이번 검수 대상: {returnReviewOrder?.pendingReturnQuantity}개</p>
        <p className="mb-0">표시된 수량을 모두 검수한 뒤 처리해 주세요. 판매 가능한 반품만 재고로 복원하세요.</p>
      </ConfirmModal>
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
      {selectedId !== null && <SmartStoreOrderDetailModal
        key={selectedId}
        adminKey={adminKey}
        productOrderId={selectedId}
        onAuthError={onAuthError}
        onClose={() => setSelectedId(null)}
        onChanged={invalidate}
      />}
      {bulkDispatchIds !== null && <BulkDispatchModal
        adminKey={adminKey}
        productOrderIds={bulkDispatchIds}
        onAuthError={onAuthError}
        onClose={() => setBulkDispatchIds(null)}
        onCompleted={(result) => {
          setBulkDispatchIds(null);
          setBulkResult(result);
          setSelectedIds(new Set(result.failures.map((failure) => failure.productOrderId)));
          invalidate();
        }}
      />}
    </>
  );
}
