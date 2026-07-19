import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, ButtonGroup, Form, Modal, Table } from "react-bootstrap";
import { adjustInventory, fetchInventoryAdjustments } from "./api";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { formatDateTime } from "@/shared/lib";
import type { InventoryAdjustmentType, ProductResponse } from "@/shared/types";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";

interface Props {
  adminKey: string;
  product: ProductResponse | null;
  onClose: () => void;
  onAuthError: () => void;
}

export function InventoryAdjustmentModal({ adminKey, product, onClose, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [type, setType] = useState<InventoryAdjustmentType>("INCREASE");
  const [quantity, setQuantity] = useState("1");
  const [reason, setReason] = useState("");

  const historyQuery = useQuery({
    queryKey: ["admin", "products", product?.id, "inventory-adjustments"],
    queryFn: () => fetchInventoryAdjustments(adminKey, product!.id),
    enabled: product !== null,
  });

  useEffect(() => {
    if (historyQuery.error instanceof ApiError && historyQuery.error.status === 401) {
      onAuthError();
    }
  }, [historyQuery.error, onAuthError]);

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => adjustInventory(adminKey, product!.id, {
      type,
      quantity: Number(quantity),
      reason,
    }),
    onSuccess: (adjustment) => {
      toast.show(`재고를 ${adjustment.quantityBefore}개에서 ${adjustment.quantityAfter}개로 조정했습니다.`);
      setQuantity("1");
      setReason("");
      queryClient.invalidateQueries({ queryKey: ["admin", "products"] });
      queryClient.invalidateQueries({
        queryKey: ["admin", "products", product?.id, "inventory-adjustments"],
      });
    },
  });

  const valid = Number.isSafeInteger(Number(quantity))
    && Number(quantity) > 0
    && reason.trim().length > 0
    && reason.length <= 500;

  const close = () => {
    mutation.reset();
    setType("INCREASE");
    setQuantity("1");
    setReason("");
    onClose();
  };

  return (
    <Modal show={product !== null} onHide={close} centered size="lg">
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">{product?.name} 재고 조정</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <div className="d-flex align-items-center justify-content-between mb-3">
          <span className="text-muted">현재 재고</span>
          <strong>{product?.quantity ?? 0}개</strong>
        </div>

        <ErrorAlert error={mutation.error} />
        <Form
          onSubmit={(event) => {
            event.preventDefault();
            if (valid) mutation.mutate();
          }}
          className="mb-4"
        >
          <Form.Label>조정 방향</Form.Label>
          <ButtonGroup className="d-flex mb-3">
            <Button
              type="button"
              variant={type === "INCREASE" ? "primary" : "outline-secondary"}
              onClick={() => setType("INCREASE")}
            >
              입고·복구
            </Button>
            <Button
              type="button"
              variant={type === "DECREASE" ? "danger" : "outline-secondary"}
              onClick={() => setType("DECREASE")}
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
              onChange={(event) => setQuantity(event.target.value)}
            />
          </Form.Group>

          <Form.Group className="mb-3" controlId="inventory-adjustment-reason">
            <Form.Label>조정 사유</Form.Label>
            <Form.Control
              value={reason}
              maxLength={500}
              onChange={(event) => setReason(event.target.value)}
              placeholder="예: 오프라인 매장 판매, 신규 작품 입고"
            />
          </Form.Group>

          <Button type="submit" disabled={!valid || mutation.isPending}>
            {mutation.isPending ? "반영 중..." : "재고 반영"}
          </Button>
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
