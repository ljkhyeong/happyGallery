import { useState } from "react";
import { Card, Badge, Button, Form, InputGroup } from "react-bootstrap";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { fetchAdminInquiries, replyInquiry } from "./api";
import type { AdminInquiryResponse } from "./api";
import { ErrorAlert, LoadingSpinner, EmptyState, useToast } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

interface Props {
  token: string;
  onAuthError: () => void;
}

export function AdminInquirySection({ token, onAuthError: _onAuthError }: Props) {
  const { data: inquiries, isLoading, error } = useQuery({
    queryKey: ["admin", "inquiries"],
    queryFn: () => fetchAdminInquiries(token),
  });

  return (
    <div>
      <h5 className="mb-3">1:1 문의 관리</h5>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {inquiries && inquiries.length === 0 && <EmptyState message="문의가 없습니다." />}
      {inquiries?.map((inq) => (
        <AdminInquiryItem key={inq.id} inquiry={inq} token={token} />
      ))}
    </div>
  );
}

function AdminInquiryItem({
  inquiry,
  token,
}: {
  inquiry: AdminInquiryResponse;
  token: string;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [replyText, setReplyText] = useState("");

  const mutation = useMutation({
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
