import { useState } from "react";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import type {
  ResolveSmartStoreInventoryRequestAction,
  SmartStoreChannelOrderResponse,
} from "@/generated/api/adminOrder";
import { adminProductsQuery } from "@/features/admin-product/api";
import { productVariantLabel } from "@/features/admin-product/productVariantLabel";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ErrorAlert, useToast } from "@/shared/ui";
import { resolveSmartStoreOrderInventory } from "./api";

export function InventoryResolutionModal({
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
  const productsQuery = useAdminQuery(onAuthError, adminProductsQuery(adminKey));
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
        : "스마트스토어 주문의 상품 연결과 재고 반영 방법을 저장했습니다.");
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
          <Modal.Title className="fs-6">스마트스토어 주문 재고 반영 방법 지정</Modal.Title>
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
            <Form.Label>해피갤러리 상품</Form.Label>
            <Form.Select
              aria-label="해피갤러리 상품"
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
            <Form.Label>재고 반영 방법</Form.Label>
            <Form.Select
              aria-label="재고 반영 방법"
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
              placeholder="상품 연결과 재고 반영 방법을 선택한 근거를 입력하세요."
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" disabled={submit.isPending} onClick={onClose}>취소</Button>
          <Button type="submit" disabled={!valid || submit.isPending}>
            {submit.isPending ? "저장 중..." : "상품 연결과 재고 반영 방법 저장"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
