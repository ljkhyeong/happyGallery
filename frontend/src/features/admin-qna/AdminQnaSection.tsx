import { useState } from "react";
import { Card, Badge, Button, Form, InputGroup } from "react-bootstrap";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchAdminQna, replyQna } from "./api";
import type { AdminQnaResponse } from "./api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  token: string;
  onAuthError: () => void;
}

export function AdminQnaSection({ token, onAuthError: _onAuthError }: Props) {
  const [productId, setProductId] = useState("");
  const [searchId, setSearchId] = useState<number | null>(null);

  const { data: qnaList, isLoading, error } = useQuery({
    queryKey: ["admin", "qna", searchId],
    queryFn: () => fetchAdminQna(searchId!, token),
    enabled: searchId !== null,
  });

  return (
    <div>
      <h5 className="mb-3">Q&A 관리</h5>
      <Form
        className="mb-3 d-flex gap-2"
        onSubmit={(e) => {
          e.preventDefault();
          const id = Number(productId);
          if (id > 0) setSearchId(id);
        }}
      >
        <Form.Control
          placeholder="상품 ID"
          value={productId}
          onChange={(e) => setProductId(e.target.value)}
          style={{ width: 120 }}
        />
        <Button type="submit" size="sm" variant="outline-primary">
          조회
        </Button>
      </Form>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {qnaList && qnaList.length === 0 && <EmptyState message="Q&A가 없습니다." />}

      {qnaList?.map((qna) => (
        <AdminQnaItem key={qna.id} qna={qna} token={token} productId={searchId!} />
      ))}
    </div>
  );
}

function AdminQnaItem({
  qna,
  token,
  productId,
}: {
  qna: AdminQnaResponse;
  token: string;
  productId: number;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [replyText, setReplyText] = useState("");

  const mutation = useMutation({
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
