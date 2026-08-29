import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Form, Table } from "react-bootstrap";
import type {
  SmartStoreChannelOrderResponse,
  SmartStoreChannelOrderResponseAttentionReason,
} from "@/generated/api/adminOrder";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  fetchSmartStoreChannelOrders,
  resolveSmartStoreReturn,
  retrySmartStoreOrderInventory,
} from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  attentionOnly?: boolean;
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

export function SmartStoreChannelOrderSection({
  adminKey,
  onAuthError,
  attentionOnly: initialAttentionOnly = false,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [attentionOnly, setAttentionOnly] = useState(initialAttentionOnly);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const queryKey = ["admin", "smartstore-orders", attentionOnly] as const;
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchSmartStoreChannelOrders(adminKey, attentionOnly),
    refetchInterval: 30_000,
  });

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
    mutationFn: ({ productOrderId, restoreStock }: {
      productOrderId: string;
      restoreStock: boolean;
    }) => resolveSmartStoreReturn(adminKey, productOrderId, restoreStock),
    onMutate: ({ productOrderId }) => setPendingId(productOrderId),
    onSuccess: (_, variables) => {
      toast.show(variables.restoreStock
        ? "반품 검수 수량을 내부 재고에 복원했습니다."
        : "판매 불가 반품으로 확인하고 재고를 복원하지 않았습니다.");
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }

  return (
    <>
      {!initialAttentionOnly && (
        <Form.Check
          className="mb-3"
          type="switch"
          id="smartstore-order-attention-only"
          label="확인이 필요한 주문만 보기"
          checked={attentionOnly}
          onChange={(event) => setAttentionOnly(event.target.checked)}
        />
      )}
      {!data?.length ? (
        <EmptyState message={attentionOnly
          ? "확인이 필요한 스마트스토어 주문이 없습니다."
          : "수집된 스마트스토어 주문이 없습니다."} />
      ) : (
        <Table responsive hover size="sm" className="align-middle">
          <thead>
            <tr>
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
            {data.map((order) => (
              <tr key={order.productOrderId}>
                <td className="small">{order.productOrderId}</td>
                <td>
                  <div>{order.productName}</div>
                  {order.productOption && (
                    <div className="small text-muted-soft">{order.productOption}</div>
                  )}
                </td>
                <td>{STATUS_LABELS[order.productOrderStatus] ?? order.productOrderStatus}</td>
                <td>{order.initialQuantity}개 / 잔여 {order.remainQuantity}개</td>
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
                <td>{actions(order, pendingId, retryInventory.mutate, resolveReturn.mutate)}</td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </>
  );
}

function actions(
  order: SmartStoreChannelOrderResponse,
  pendingId: string | null,
  retry: (productOrderId: string) => void,
  resolveReturn: (variables: { productOrderId: string; restoreStock: boolean }) => void,
) {
  const disabled = pendingId === order.productOrderId;
  if (order.attentionReason === "MAPPING_REQUIRED"
      || order.attentionReason === "STOCK_SHORTAGE") {
    return (
      <Button
        size="sm"
        variant="outline-primary"
        disabled={disabled}
        onClick={() => retry(order.productOrderId)}
      >
        재고 반영 다시 시도
      </Button>
    );
  }
  if (order.attentionReason !== "RETURN_REVIEW") return null;
  return (
    <div className="d-flex flex-wrap gap-1">
      <Button
        size="sm"
        variant="outline-success"
        disabled={disabled}
        onClick={() => resolveReturn({ productOrderId: order.productOrderId, restoreStock: true })}
      >
        검수 후 재고 복원
      </Button>
      <Button
        size="sm"
        variant="outline-secondary"
        disabled={disabled}
        onClick={() => resolveReturn({ productOrderId: order.productOrderId, restoreStock: false })}
      >
        복원 없이 종료
      </Button>
    </div>
  );
}
