import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Form, Modal, Table } from "react-bootstrap";
import type {
  ReconcileSmartStoreOrderActionRequestOutcome,
  SmartStoreOrderActionHistoryResponse,
} from "@/generated/api/adminOrder";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  fetchCurrentSmartStoreOrderStatus,
  fetchUnresolvedSmartStoreOrderActions,
  reconcileSmartStoreOrderActionHistory,
} from "./api";
import { ACTION_LABELS, ACTION_STATUS_LABELS } from "./labels";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SmartStoreOrderReconciliationSection({ adminKey, onAuthError }: Props) {
  const [pageCursors, setPageCursors] = useState<(string | undefined)[]>([undefined]);
  const [selected, setSelected] = useState<SmartStoreOrderActionHistoryResponse | null>(null);
  const cursor = pageCursors.at(-1);
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-order-actions", "unresolved", cursor ?? null],
    queryFn: () => fetchUnresolvedSmartStoreOrderActions(adminKey, cursor),
    refetchInterval: 30_000,
  });

  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) {
    if (query.error instanceof ApiError && query.error.status === 401) return null;
    return <ErrorAlert error={query.error} />;
  }

  const actions = query.data?.content ?? [];
  return (
    <>
      {!actions.length ? (
        <EmptyState message="처리 결과를 확인할 스마트스토어 주문 요청이 없습니다." />
      ) : (
        <Table responsive hover size="sm" className="align-middle">
          <thead>
            <tr>
              <th>상품 주문 번호</th>
              <th>요청</th>
              <th>상태</th>
              <th>요청자·시각</th>
              <th>요청 내용</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {actions.map((action) => (
              <tr key={action.id}>
                <td className="small">{action.productOrderId}</td>
                <td>{ACTION_LABELS[action.action] ?? action.action}</td>
                <td>
                  <Badge bg="warning" text="dark">
                    {action.status === "REQUESTED"
                      ? "5분 이상 완료되지 않음"
                      : ACTION_STATUS_LABELS[action.status] ?? action.status}
                  </Badge>
                </td>
                <td className="small">
                  <div>{action.changedBy}</div>
                  <div className="text-muted-soft">{formatDateTime(action.requestedAt)}</div>
                </td>
                <td className="small">{action.requestSummary ?? "추가 입력 없음"}</td>
                <td>
                  <Button size="sm" variant="outline-primary" onClick={() => setSelected(action)}>
                    네이버 상태 확인
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
      {(pageCursors.length > 1 || query.data?.hasMore) && (
        <div className="d-flex justify-content-end gap-2">
          <Button size="sm" variant="outline-secondary" disabled={pageCursors.length === 1}
            onClick={() => setPageCursors((current) => current.slice(0, -1))}>
            이전
          </Button>
          <Button size="sm" variant="outline-secondary"
            disabled={!query.data?.hasMore || !query.data.nextCursor}
            onClick={() => {
              if (query.data?.nextCursor) {
                setPageCursors((current) => [...current, query.data?.nextCursor ?? undefined]);
              }
            }}>
            다음
          </Button>
        </div>
      )}
      <ReconciliationModal
        key={selected?.id ?? "closed"}
        adminKey={adminKey}
        action={selected}
        onAuthError={onAuthError}
        onClose={() => setSelected(null)}
      />
    </>
  );
}

function ReconciliationModal({
  adminKey,
  action,
  onAuthError,
  onClose,
}: Props & {
  action: SmartStoreOrderActionHistoryResponse | null;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [outcome, setOutcome] = useState<ReconcileSmartStoreOrderActionRequestOutcome>("APPLIED");
  const [note, setNote] = useState("");
  const currentStatus = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-orders", action?.productOrderId, "current-status"],
    queryFn: () => fetchCurrentSmartStoreOrderStatus(adminKey, action!.productOrderId),
    enabled: action !== null,
    retry: false,
  });
  const reconcile = useAdminMutation(onAuthError, {
    mutationFn: () => reconcileSmartStoreOrderActionHistory(
      adminKey,
      action!.id,
      { outcome, note: note.trim() },
    ),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-order-actions", "unresolved"],
      });
      toast.show(outcome === "APPLIED"
        ? "네이버에 반영된 요청으로 확인했습니다."
        : "네이버에 반영되지 않은 요청으로 확인했습니다.");
      onClose();
    },
  });
  const status = currentStatus.data;

  return (
    <Modal show={action !== null} onHide={() => { if (!reconcile.isPending) onClose(); }} centered>
      <Form onSubmit={(event) => {
        event.preventDefault();
        if (action && note.trim()) reconcile.mutate();
      }}>
        <Modal.Header closeButton={!reconcile.isPending}>
          <Modal.Title className="fs-6">스마트스토어 주문 처리 결과 확인</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={currentStatus.error} />
          <ErrorAlert error={reconcile.error} />
          {action && (
            <div className="small mb-3">
              <div className="fw-semibold">{ACTION_LABELS[action.action] ?? action.action}</div>
              <div>상품 주문 번호: {action.productOrderId}</div>
              {action.requestSummary && <div className="text-muted-soft">{action.requestSummary}</div>}
              {action.resultMessage && <div className="text-warning-emphasis">{action.resultMessage}</div>}
            </div>
          )}
          {currentStatus.isLoading ? <LoadingSpinner /> : status ? (
            <Alert variant="light" className="small border">
              <div className="fw-semibold mb-1">네이버 현재 상태</div>
              <div>주문: {status.productOrderStatus ?? "확인되지 않음"}</div>
              <div>발주: {status.placeOrderStatus ?? "확인되지 않음"}</div>
              <div>클레임: {[status.claimType, status.claimStatus].filter(Boolean).join(" / ") || "없음"}</div>
              <div>잔여 수량: {status.remainQuantity}개</div>
              <div>배송: {[status.deliveryCompany, status.trackingNumber].filter(Boolean).join(" / ") || "없음"}</div>
            </Alert>
          ) : null}
          <Alert variant="warning" className="small">
            자동으로 같은 요청을 다시 보내지 않습니다. 위 상태나 네이버 판매자센터에서 실제 반영 여부를 확인한 뒤 선택하세요.
          </Alert>
          <Form.Group className="mb-3">
            <Form.Label>확인 결과</Form.Label>
            <Form.Select aria-label="대사 결과" value={outcome}
              onChange={(event) => setOutcome(
                event.target.value as ReconcileSmartStoreOrderActionRequestOutcome,
              )}>
              <option value="APPLIED">네이버에 반영됨</option>
              <option value="NOT_APPLIED">네이버에 반영되지 않음</option>
            </Form.Select>
          </Form.Group>
          <Form.Group>
            <Form.Label>확인 근거</Form.Label>
            <Form.Control as="textarea" rows={3} required maxLength={500}
              aria-label="대사 근거" value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="확인한 주문 상태, 운송장 또는 판매자센터 처리 결과를 입력하세요." />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" disabled={reconcile.isPending} onClick={onClose}>닫기</Button>
          <Button type="submit" disabled={!note.trim() || reconcile.isPending}>
            {reconcile.isPending ? "저장 중..." : "확인 결과 저장"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
