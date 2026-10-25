import { Button } from "react-bootstrap";
import type { SmartStoreChannelOrderResponse } from "@/generated/api/adminOrder";
import { adminProductHref } from "@/shared/lib/adminLinks";
import { LinkButton } from "@/shared/ui";

export function OrderRowActions({
  order,
  pendingId,
  onRetry,
  onReturnReview,
  onInventoryResolution,
  onOpenDetail,
}: {
  order: SmartStoreChannelOrderResponse;
  pendingId: string | null;
  onRetry: (productOrderId: string) => void;
  onReturnReview: (order: SmartStoreChannelOrderResponse) => void;
  onInventoryResolution: (order: SmartStoreChannelOrderResponse) => void;
  onOpenDetail: (productOrderId: string) => void;
}) {
  const disabled = pendingId === order.productOrderId;
  if (order.attentionReason === "MAPPING_REQUIRED"
      || order.attentionReason === "STATUS_REVIEW") {
    return <div className="d-flex flex-wrap gap-1">
      <Button size="sm" variant="outline-secondary" onClick={() => onOpenDetail(order.productOrderId)}>
        주문 처리
      </Button>
      <Button
        size="sm"
        variant="outline-primary"
        disabled={disabled}
        onClick={() => onInventoryResolution(order)}
      >
        상품 연결·재고 반영 방법
      </Button>
    </div>;
  }
  if (order.attentionReason === "STOCK_SHORTAGE") {
    return <div className="d-flex flex-wrap gap-1">
      <Button size="sm" variant="outline-secondary" onClick={() => onOpenDetail(order.productOrderId)}>
        주문 처리
      </Button>
      {order.productId && <LinkButton
        size="sm"
        variant="outline-warning"
        to={adminProductHref(order.productId, order.productVariantId)}
      >
        재고 조정
      </LinkButton>}
      <Button size="sm" variant="outline-primary" disabled={disabled}
        onClick={() => onRetry(order.productOrderId)}>
        재고 반영 다시 시도
      </Button>
    </div>;
  }
  if (order.attentionReason !== "RETURN_REVIEW") {
    return <Button
      size="sm"
      variant="outline-secondary"
      onClick={() => onOpenDetail(order.productOrderId)}
    >
      주문 처리
    </Button>;
  }
  return (
    <div className="d-flex flex-wrap gap-1">
      <Button
        size="sm"
        variant="outline-secondary"
        onClick={() => onOpenDetail(order.productOrderId)}
      >
        주문 처리
      </Button>
      <Button
        size="sm"
        variant="outline-success"
        disabled={disabled}
        onClick={() => onReturnReview(order)}
      >
        반품 검수
      </Button>
    </div>
  );
}
