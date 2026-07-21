import { useState } from "react";
import { Card, Badge, Button, Form, InputGroup } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { fetchAdminQna, replyQna } from "./api";
import { fetchProducts } from "@/features/admin-product/api";
import type { AdminQnaResponse } from "./api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";

interface Props {
  token: string;
  onAuthError: () => void;
}

export function AdminQnaSection({ token, onAuthError }: Props) {
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);

  const productsQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(token),
  });

  const { data: qnaList, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "qna", selectedProductId],
    queryFn: () => fetchAdminQna(selectedProductId!, token),
    enabled: selectedProductId !== null,
  });

  return (
    <div>
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

      {productsQuery.isLoading && <LoadingSpinner text="상품을 불러오는 중..." />}
      <ErrorAlert error={productsQuery.error} />
      {productsQuery.data?.length === 0 && <EmptyState message="등록된 상품이 없습니다." />}
      {selectedProductId === null && productsQuery.data && productsQuery.data.length > 0 && (
        <EmptyState message="상품을 선택하면 Q&A를 확인할 수 있습니다." />
      )}
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {qnaList && qnaList.length === 0 && <EmptyState message="Q&A가 없습니다." />}

      {qnaList?.map((qna) => (
        <AdminQnaItem
          key={qna.id}
          qna={qna}
          token={token}
          productId={selectedProductId!}
          onAuthError={onAuthError}
        />
      ))}
    </div>
  );
}

function AdminQnaItem({
  qna,
  token,
  productId,
  onAuthError,
}: {
  qna: AdminQnaResponse;
  token: string;
  productId: number;
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
      queryClient.invalidateQueries({ queryKey: ["admin", "qna", productId] });
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
              {qna.authorName} (ID: {qna.userId}) | {formatDateTime(qna.createdAt)}
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
