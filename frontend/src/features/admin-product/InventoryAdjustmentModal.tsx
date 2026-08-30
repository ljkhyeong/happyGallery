import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, ButtonGroup, Form, Modal, Table } from "react-bootstrap";
import { adjustInventory, fetchInventoryAdjustments } from "./api";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import type { InventoryAdjustmentType, ProductResponse } from "@/shared/types";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  product: ProductResponse | null;
  initialVariantId?: number;
  onClose: () => void;
  onAuthError: () => void;
}

export function InventoryAdjustmentModal({
  adminKey,
  product,
  initialVariantId,
  onClose,
  onAuthError,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [type, setType] = useState<InventoryAdjustmentType>("INCREASE");
  const [quantity, setQuantity] = useState("1");
  const [reason, setReason] = useState("");
  const [showImpactConfirm, setShowImpactConfirm] = useState(false);
  const [productVariantId, setProductVariantId] = useState<number | null>(null);

  useEffect(() => {
    setProductVariantId(product?.type === "MADE_TO_ORDER"
      ? (product.variants.find((variant) => variant.id === initialVariantId)?.id
        ?? product.variants.find((variant) => variant.active)?.id
        ?? product.variants[0]?.id
        ?? null)
      : null);
  }, [initialVariantId, product]);

  const historyQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products", product?.id, "inventory-adjustments"],
    queryFn: () => fetchInventoryAdjustments(adminKey, product!.id),
    enabled: product !== null,
  });

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => adjustInventory(adminKey, product!.id, {
      productVariantId,
      type,
      quantity: Number(quantity),
      reason,
    }),
    onSuccess: (adjustment) => {
      toast.show(`재고를 ${adjustment.quantityBefore}개에서 ${adjustment.quantityAfter}개로 조정했습니다.`);
      setShowImpactConfirm(false);
      setQuantity("1");
      setReason("");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({
        queryKey: ["admin", "products", product?.id, "inventory-adjustments"],
      });
    },
  });

  const quantityNumber = Number(quantity);
  const selectedVariant = product?.variants.find((variant) => variant.id === productVariantId);
  const currentQuantity = mutation.data?.productVariantId === productVariantId
    ? mutation.data.quantityAfter
    : (selectedVariant?.quantity ?? product?.quantity ?? 0);
  const projectedQuantity = type === "INCREASE"
    ? currentQuantity + quantityNumber
    : currentQuantity - quantityNumber;
  const quantityExceedsStock = Number.isSafeInteger(quantityNumber)
    && quantityNumber > 0
    && projectedQuantity < 0;
  const valid = Number.isSafeInteger(quantityNumber)
    && quantityNumber > 0
    && projectedQuantity >= 0
    && (product?.type !== "MADE_TO_ORDER" || productVariantId !== null)
    && reason.trim().length > 0
    && reason.length <= 500;

  const close = () => {
    mutation.reset();
    setType("INCREASE");
    setQuantity("1");
    setReason("");
    setShowImpactConfirm(false);
    onClose();
  };

  return (
    <Modal
      show={product !== null}
      aria-labelledby="admin-inventory-adjustment-title"
      onHide={close}
      centered
      size="lg"
    >
      <Modal.Header closeButton>
        <Modal.Title id="admin-inventory-adjustment-title" className="fs-6">
          {product?.name} 재고 조정
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        {product?.type === "MADE_TO_ORDER" && (
          <Form.Group className="mb-3" controlId="inventory-adjustment-variant">
            <Form.Label>옵션 조합</Form.Label>
            <Form.Select
              value={productVariantId ?? ""}
              onChange={(event) => {
                setProductVariantId(Number(event.target.value));
                mutation.reset();
                setShowImpactConfirm(false);
              }}
            >
              {product.variants.map((variant) => (
                <option key={variant.id} value={variant.id}>
                  {variant.selections.length === 0
                    ? "기본 조합"
                    : variant.selections.map((selection) => {
                      const group = product.optionGroups.find(
                        (candidate) => candidate.key === selection.groupKey,
                      );
                      const value = group?.values.find(
                        (candidate) => candidate.key === selection.valueKey,
                      );
                      return `${group?.name ?? "옵션"}: ${value?.name ?? "값"}`;
                    }).join(" / ")} ({variant.quantity}개)
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        )}
        <div className="d-flex align-items-center justify-content-between mb-3">
          <span className="text-muted">현재 재고</span>
          <strong>{currentQuantity}개</strong>
        </div>

        <ErrorAlert error={mutation.error} />
        <Form
          onSubmit={(event) => {
            event.preventDefault();
            if (valid) setShowImpactConfirm(true);
          }}
          className="mb-4"
        >
          <Form.Label>조정 방향</Form.Label>
          <ButtonGroup className="d-flex mb-3">
            <Button
              type="button"
              variant={type === "INCREASE" ? "primary" : "outline-secondary"}
              onClick={() => {
                setType("INCREASE");
                setShowImpactConfirm(false);
              }}
            >
              입고·복구
            </Button>
            <Button
              type="button"
              variant={type === "DECREASE" ? "danger" : "outline-secondary"}
              onClick={() => {
                setType("DECREASE");
                setShowImpactConfirm(false);
              }}
            >
              오프라인 판매·폐기
            </Button>
          </ButtonGroup>

          <Form.Group className="mb-3" controlId="inventory-adjustment-quantity">
            <Form.Label>수량</Form.Label>
            <Form.Control
              type="number"
              min={1}
              step={1}
              value={quantity}
              isInvalid={quantityExceedsStock}
              aria-invalid={quantityExceedsStock}
              aria-describedby={
                quantityExceedsStock ? "inventory-adjustment-quantity-error" : undefined
              }
              onChange={(event) => {
                setQuantity(event.target.value);
                setShowImpactConfirm(false);
              }}
            />
            <Form.Control.Feedback
              id="inventory-adjustment-quantity-error"
              type="invalid"
            >
              현재 재고보다 많이 감소시킬 수 없습니다.
            </Form.Control.Feedback>
          </Form.Group>

          <Form.Group className="mb-3" controlId="inventory-adjustment-reason">
            <Form.Label>조정 사유</Form.Label>
            <Form.Control
              value={reason}
              maxLength={500}
              onChange={(event) => {
                setReason(event.target.value);
                setShowImpactConfirm(false);
              }}
              placeholder="예: 오프라인 매장 판매, 신규 작품 입고"
            />
          </Form.Group>

          {showImpactConfirm ? (
            <Alert variant={type === "DECREASE" ? "warning" : "info"} className="mb-0">
              <div className="fw-semibold mb-1">재고 변화 확인</div>
              <div className="small mb-3">
                {currentQuantity}개에서 <strong>{projectedQuantity}개</strong>로 변경됩니다.
                {type === "DECREASE" && " 감소한 수량은 고객이 새로 주문할 수 없게 됩니다."}
              </div>
              <div className="d-flex gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="outline-secondary"
                  disabled={mutation.isPending}
                  onClick={() => setShowImpactConfirm(false)}
                >
                  다시 입력
                </Button>
                <Button
                  type="button"
                  size="sm"
                  variant={type === "DECREASE" ? "danger" : "primary"}
                  disabled={mutation.isPending}
                  onClick={() => mutation.mutate()}
                >
                  {mutation.isPending ? "반영 중..." : "확인하고 재고 반영"}
                </Button>
              </div>
            </Alert>
          ) : (
            <Button type="submit" disabled={!valid || mutation.isPending}>
              영향 확인
            </Button>
          )}
        </Form>

        <h6>최근 조정 이력</h6>
        {historyQuery.isLoading && <LoadingSpinner />}
        {historyQuery.error && !(historyQuery.error instanceof ApiError && historyQuery.error.status === 401)
          && <ErrorAlert error={historyQuery.error} />}
        {historyQuery.data?.length === 0 && <EmptyState message="재고 조정 이력이 없습니다." />}
        {historyQuery.data && historyQuery.data.length > 0 && (
          <Table responsive size="sm" className="mb-0">
            <thead>
              <tr>
                <th>시각</th>
                <th>구분</th>
                <th>변경</th>
                <th>사유</th>
                <th>처리자</th>
              </tr>
            </thead>
            <tbody>
              {historyQuery.data.map((adjustment) => (
                <tr key={adjustment.id}>
                  <td className="text-nowrap small">{formatDateTime(adjustment.adjustedAt)}</td>
                  <td>
                    <Badge bg={adjustment.type === "INCREASE" ? "primary" : "danger"}>
                      {adjustment.type === "INCREASE" ? "증가" : "감소"}
                    </Badge>
                  </td>
                  <td className="text-nowrap">
                    {adjustment.quantityBefore} → {adjustment.quantityAfter}
                  </td>
                  <td>{adjustment.reason}</td>
                  <td className="text-nowrap">{adjustment.adjustedBy}</td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
      </Modal.Body>
    </Modal>
  );
}
