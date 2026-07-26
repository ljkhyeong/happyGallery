import { useState } from "react";
import { Card, Badge, Button, ButtonGroup, Form, InputGroup } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { fetchAdminQna, fetchUnansweredAdminQna, replyQna } from "./api";
import { fetchProducts } from "@/features/admin-product/api";
import type { AdminQnaResponse } from "./api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";

interface Props {
  token: string;
  onAuthError: () => void;
}

export function AdminQnaSection({ token, onAuthError }: Props) {
  const [view, setView] = useState<"UNANSWERED" | "PRODUCT">("UNANSWERED");
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const {
    cursor,
    hasPreviousPage,
    showNextPage,
    showPreviousPage,
  } = useCursorHistory();

  const productsQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(token),
  });

  const unansweredQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "qna", "unanswered", cursor],
    queryFn: () => fetchUnansweredAdminQna(token, cursor),
    enabled: view === "UNANSWERED",
  });

  const productQnaQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "qna", selectedProductId],
    queryFn: () => fetchAdminQna(selectedProductId!, token),
    enabled: view === "PRODUCT" && selectedProductId !== null,
  });
  const unansweredPage = unansweredQuery.data;
  const qnaList = view === "UNANSWERED"
    ? unansweredPage?.content
    : productQnaQuery.data;
  const isLoading = view === "UNANSWERED"
    ? unansweredQuery.isLoading
    : productQnaQuery.isLoading;
  const error = view === "UNANSWERED"
    ? unansweredQuery.error
    : productQnaQuery.error;
  const productNames = new Map(
    productsQuery.data?.map((product) => [product.id, product.name]) ?? [],
  );

  return (
    <div>
      <ButtonGroup size="sm" className="mb-3" aria-label="Q&A 조회 범위">
        <Button
          variant={view === "UNANSWERED" ? "dark" : "outline-secondary"}
          onClick={() => setView("UNANSWERED")}
        >
          미답변
        </Button>
        <Button
          variant={view === "PRODUCT" ? "dark" : "outline-secondary"}
          onClick={() => setView("PRODUCT")}
        >
          상품별
        </Button>
      </ButtonGroup>

      {view === "PRODUCT" && (
        <Form.Group className="admin-qna-product-filter mb-3" controlId="admin-qna-product">
          <Form.Label>상품</Form.Label>
          <Form.Select
            value={selectedProductId ?? ""}
            disabled={productsQuery.isLoading || !productsQuery.data?.length}
            onChange={(event) => {
              const value = event.target.value;
              setSelectedProductId(value ? Number(value) : null);
            }}
          >
            <option value="">상품을 선택하세요</option>
            {productsQuery.data?.map((product) => (
              <option key={product.id} value={product.id}>{product.name}</option>
            ))}
          </Form.Select>
        </Form.Group>
      )}

      {productsQuery.isLoading && <LoadingSpinner text="상품을 불러오는 중..." />}
      <ErrorAlert error={productsQuery.error} />
      {productsQuery.data?.length === 0 && <EmptyState message="등록된 상품이 없습니다." />}
      {view === "PRODUCT"
        && selectedProductId === null
        && productsQuery.data
        && productsQuery.data.length > 0 && (
        <EmptyState message="상품을 선택하면 Q&A를 확인할 수 있습니다." />
      )}
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {qnaList && qnaList.length === 0 && (
        <EmptyState message={view === "UNANSWERED" ? "답변을 기다리는 Q&A가 없습니다." : "Q&A가 없습니다."} />
      )}

      {qnaList?.map((qna) => (
        <AdminQnaItem
          key={qna.id}
          qna={qna}
          token={token}
          productName={productNames.get(qna.productId)}
          onAuthError={onAuthError}
        />
      ))}
      {view === "UNANSWERED" && (hasPreviousPage || unansweredPage?.hasMore) && (
        <div className="d-flex justify-content-center gap-2 mt-3">
          <Button
            size="sm"
            variant="outline-secondary"
            disabled={!hasPreviousPage || isLoading}
            onClick={showPreviousPage}
          >
            이전
          </Button>
          <Button
            size="sm"
            variant="outline-primary"
            disabled={!unansweredPage?.hasMore || isLoading}
            onClick={() => showNextPage(unansweredPage?.nextCursor)}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  );
}

function AdminQnaItem({
  qna,
  token,
  productName,
  onAuthError,
}: {
  qna: AdminQnaResponse;
  token: string;
  productName?: string;
  onAuthError: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [replyText, setReplyText] = useState("");

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => replyQna(qna.id, replyText, token),
    onSuccess: () => {
      toast.show("답변이 등록되었습니다.");
      setReplyText("");
      queryClient.invalidateQueries({ queryKey: ["admin", "qna"] });
    },
  });

  return (
    <Card className="mb-2">
      <Card.Body className="py-2 px-3">
        <div className="d-flex justify-content-between align-items-start">
          <div>
            <div className="d-flex align-items-center gap-2 mb-1">
              {qna.secret && <Badge bg="secondary" className="badge-sm">비밀글</Badge>}
              {qna.replyContent ? (
                <Badge bg="info" className="badge-sm">답변완료</Badge>
              ) : (
                <Badge bg="warning" className="badge-sm">답변대기</Badge>
              )}
              <span className="fw-semibold small">{qna.title}</span>
            </div>
            <div className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
              {productName ?? `상품 #${qna.productId}`} | {qna.authorName} (ID: {qna.userId}) | {formatDateTime(qna.createdAt)}
            </div>
          </div>
        </div>
        <div className="mt-2 small bg-light p-2 rounded">{qna.content}</div>

        {qna.replyContent && (
          <div className="mt-2 p-2 rounded small" style={{ background: "#f0f4ff" }}>
            <strong>답변:</strong> {qna.replyContent}
          </div>
        )}

        {!qna.replyContent && (
          <Form
            className="mt-2"
            onSubmit={(e) => {
              e.preventDefault();
              mutation.mutate();
            }}
          >
            <InputGroup size="sm">
              <Form.Control
                placeholder="답변을 입력하세요"
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
              />
              <Button
                type="submit"
                variant="primary"
                disabled={!replyText.trim() || mutation.isPending}
              >
                답변
              </Button>
            </InputGroup>
            <ErrorAlert error={mutation.error} />
          </Form>
        )}
      </Card.Body>
    </Card>
  );
}
