import { useState } from "react";
import { Badge, Button, Card, Form } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import { answerChannelQna, fetchSmartStoreInquiries } from "./api";
import type { SmartStoreInquiryResponse } from "./api";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { CONTENT_BODY_MAX_LENGTH, contentLengthLabel } from "@/shared/validation/contentText";

interface Props {
  token: string;
  onAuthError: () => void;
}

const queryKey = ["admin", "smartstore-inquiries"] as const;

export function SmartStoreInquirySection({ token, onAuthError }: Props) {
  const [unansweredOnly, setUnansweredOnly] = useState(true);
  const query = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, unansweredOnly],
    queryFn: () => fetchSmartStoreInquiries(token, unansweredOnly),
    refetchInterval: 60_000,
  });

  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) return <ErrorAlert error={query.error} />;

  return <>
    <Form.Check
      className="mb-3"
      type="switch"
      id="smartstore-inquiry-unanswered-only"
      label="미답변 문의만 보기"
      checked={unansweredOnly}
      onChange={(event) => setUnansweredOnly(event.target.checked)}
    />
    {!query.data?.length ? (
      <EmptyState message={unansweredOnly
        ? "답변을 기다리는 스마트스토어 상품 문의가 없습니다."
        : "최근 스마트스토어 상품 문의가 없습니다."} />
    ) : query.data.map((inquiry) => (
      <SmartStoreInquiryCard
        key={inquiry.questionId}
        inquiry={inquiry}
        token={token}
        onAuthError={onAuthError}
      />
    ))}
  </>;
}

function SmartStoreInquiryCard({
  inquiry,
  token,
  onAuthError,
}: {
  inquiry: SmartStoreInquiryResponse;
  token: string;
  onAuthError: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [content, setContent] = useState("");
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => answerChannelQna(inquiry.questionId, content, token),
    onSuccess: async () => {
      setContent("");
      toast.show("스마트스토어 상품 문의에 답변했습니다.");
      await queryClient.invalidateQueries({ queryKey });
    },
  });

  return <Card className="mb-2">
    <Card.Body className="py-2 px-3">
      <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
        <Badge bg={inquiry.answered ? "info" : "warning"} text={inquiry.answered ? undefined : "dark"}>
          {inquiry.answered ? "답변 완료" : "답변 대기"}
        </Badge>
        <span className="fw-semibold small">{inquiry.productName}</span>
      </div>
      <div className="small text-muted-soft mb-2">
        {inquiry.maskedWriterId} · {formatDateTime(inquiry.createdAt)} · 채널 상품 {inquiry.channelProductId}
      </div>
      <div className="small bg-light rounded p-2">{inquiry.question}</div>
      {inquiry.answer ? (
        <div className="small rounded p-2 mt-2" style={{ background: "#f0f4ff" }}>
          <strong>답변:</strong> {inquiry.answer}
        </div>
      ) : (
        <Form className="mt-2" onSubmit={(event) => {
          event.preventDefault();
          mutation.mutate();
        }}>
          <Form.Control
            as="textarea"
            rows={3}
            maxLength={CONTENT_BODY_MAX_LENGTH}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="스마트스토어에 등록할 답변"
          />
          <div className="d-flex justify-content-between align-items-center mt-1">
            <Form.Text>{contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}</Form.Text>
            <Button type="submit" size="sm" disabled={!content.trim() || mutation.isPending}>
              답변 등록
            </Button>
          </div>
          <ErrorAlert error={mutation.error} />
        </Form>
      )}
    </Card.Body>
  </Card>;
}
