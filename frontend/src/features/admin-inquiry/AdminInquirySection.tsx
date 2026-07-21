import { useState } from "react";
import { Card, Badge, Button, Form, InputGroup } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { fetchAdminInquiries, replyInquiry } from "./api";
import type { AdminInquiryResponse } from "./api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";

interface Props {
  token: string;
  onAuthError: () => void;
}

export function AdminInquirySection({ token, onAuthError }: Props) {
  const [cursor, setCursor] = useState<string | undefined>();
  const [cursorHistory, setCursorHistory] = useState<(string | undefined)[]>([]);
  const { data: page, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "inquiries", cursor],
    queryFn: () => fetchAdminInquiries(token, cursor),
  });
  const inquiries = page?.content;

  function showNextPage() {
    if (!page?.nextCursor) return;
    setCursorHistory((history) => [...history, cursor]);
    setCursor(page.nextCursor);
  }

  function showPreviousPage() {
    setCursor(cursorHistory[cursorHistory.length - 1]);
    setCursorHistory(cursorHistory.slice(0, -1));
  }

  return (
    <div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {inquiries && inquiries.length === 0 && <EmptyState message="문의가 없습니다." />}
      {inquiries?.map((inq) => (
        <AdminInquiryItem
          key={inq.id}
          inquiry={inq}
          token={token}
          onAuthError={onAuthError}
        />
      ))}
      {(cursorHistory.length > 0 || page?.hasMore) && (
        <div className="d-flex justify-content-center gap-2 mt-3">
          <Button
            size="sm"
            variant="outline-secondary"
            disabled={cursorHistory.length === 0 || isLoading}
            onClick={showPreviousPage}
          >
            이전
          </Button>
          <Button
            size="sm"
            variant="outline-primary"
            disabled={!page?.hasMore || isLoading}
            onClick={showNextPage}
          >
            다음
          </Button>
        </div>
      )}
    </div>
  );
}

function AdminInquiryItem({
  inquiry,
  token,
  onAuthError,
}: {
  inquiry: AdminInquiryResponse;
  token: string;
  onAuthError: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [replyText, setReplyText] = useState("");

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => replyInquiry(inquiry.id, replyText, token),
    onSuccess: () => {
      toast.show("답변이 등록되었습니다.");
      setReplyText("");
      queryClient.invalidateQueries({ queryKey: ["admin", "inquiries"] });
    },
  });

  return (
    <Card className="mb-2">
      <Card.Body className="py-2 px-3">
        <div className="d-flex justify-content-between align-items-start">
          <div>
            <div className="d-flex align-items-center gap-2 mb-1">
              {inquiry.replyContent ? (
                <Badge bg="info" className="badge-sm">답변완료</Badge>
              ) : (
                <Badge bg="warning" className="badge-sm">답변대기</Badge>
              )}
              <span className="fw-semibold small">{inquiry.title}</span>
            </div>
            <div className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
              {inquiry.userName} (ID: {inquiry.userId}) | {formatDateTime(inquiry.createdAt)}
            </div>
          </div>
        </div>
        <div className="mt-2 small bg-light p-2 rounded">{inquiry.content}</div>

        {inquiry.replyContent && (
          <div className="mt-2 p-2 rounded small" style={{ background: "#f0f4ff" }}>
            <strong>답변:</strong> {inquiry.replyContent}
          </div>
        )}

        {!inquiry.replyContent && (
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
