import { useState } from "react";
import { Badge, Button, Card, Form } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import {
  answerChannelQna,
  answerCustomerInquiry,
  fetchSmartStoreCustomerInquiries,
  fetchSmartStoreInquiries,
} from "./api";
import type { SmartStoreCustomerInquiryResponse, SmartStoreInquiryResponse } from "./api";
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
  const [inquiryType, setInquiryType] = useState<"product" | "customer">("product");
  const [unansweredOnly, setUnansweredOnly] = useState(true);
  const productQuery = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, "product", unansweredOnly],
    queryFn: () => fetchSmartStoreInquiries(token, unansweredOnly),
    enabled: inquiryType === "product",
    refetchInterval: 60_000,
  });
  const customerQuery = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, "customer", unansweredOnly],
    queryFn: () => fetchSmartStoreCustomerInquiries(token, unansweredOnly),
    enabled: inquiryType === "customer",
    refetchInterval: 60_000,
  });
  const isLoading = inquiryType === "product" ? productQuery.isLoading : customerQuery.isLoading;
  const error = inquiryType === "product" ? productQuery.error : customerQuery.error;
  const empty = inquiryType === "product"
    ? !productQuery.data?.length
    : !customerQuery.data?.length;

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorAlert error={error} />;

  return <>
    <div className="d-flex gap-2 mb-3">
      <Button
        size="sm"
        variant={inquiryType === "product" ? "primary" : "outline-secondary"}
        onClick={() => setInquiryType("product")}
      >상품 문의</Button>
      <Button
        size="sm"
        variant={inquiryType === "customer" ? "primary" : "outline-secondary"}
        onClick={() => setInquiryType("customer")}
      >주문·배송 문의</Button>
    </div>
    <Form.Check
      className="mb-3"
      type="switch"
      id="smartstore-inquiry-unanswered-only"
      label="미답변 문의만 보기"
      checked={unansweredOnly}
      onChange={(event) => setUnansweredOnly(event.target.checked)}
    />
    {empty ? (
      <EmptyState message={unansweredOnly
        ? "답변을 기다리는 스마트스토어 문의가 없습니다."
        : "최근 스마트스토어 문의가 없습니다."} />
    ) : inquiryType === "product"
      ? productQuery.data?.map((inquiry) => (
        <SmartStoreInquiryCard key={inquiry.questionId} inquiry={inquiry}
          token={token} onAuthError={onAuthError} />
      ))
      : customerQuery.data?.map((inquiry) => (
        <SmartStoreCustomerInquiryCard key={inquiry.inquiryNo} inquiry={inquiry}
          token={token} onAuthError={onAuthError} />
      ))}
  </>;
}

function SmartStoreCustomerInquiryCard({
  inquiry,
  token,
  onAuthError,
}: {
  inquiry: SmartStoreCustomerInquiryResponse;
  token: string;
  onAuthError: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [content, setContent] = useState("");
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => answerCustomerInquiry(inquiry.inquiryNo, content, token),
    onSuccess: async () => {
      setContent("");
      toast.show("스마트스토어 주문·배송 문의에 답변했습니다.");
      await queryClient.invalidateQueries({ queryKey });
    },
  });

  return <Card className="mb-2"><Card.Body className="py-2 px-3">
    <div className="d-flex flex-wrap align-items-center gap-2 mb-1">
      <Badge bg={inquiry.answered ? "info" : "warning"} text={inquiry.answered ? undefined : "dark"}>
        {inquiry.answered ? "답변 완료" : "답변 대기"}
      </Badge>
      <span className="fw-semibold small">{inquiry.title ?? inquiry.category ?? "고객 문의"}</span>
    </div>
    <div className="small text-muted-soft mb-2">
      {inquiry.customerName ?? inquiry.maskedCustomerId ?? "고객"} · {formatDateTime(inquiry.createdAt)}
      {inquiry.orderId ? ` · 주문 ${inquiry.orderId}` : ""}
    </div>
    {(inquiry.productName || inquiry.productOrderOption) && <div className="small mb-2">
      {inquiry.productName ?? "상품"}{inquiry.productOrderOption ? ` · ${inquiry.productOrderOption}` : ""}
    </div>}
    <div className="small bg-light rounded p-2">{inquiry.inquiryContent}</div>
    {inquiry.answerContent ? (
      <div className="small rounded p-2 mt-2" style={{ background: "#f0f4ff" }}>
        <strong>답변:</strong> {inquiry.answerContent}
      </div>
    ) : (
      <InquiryAnswerForm content={content} pending={mutation.isPending} error={mutation.error}
        onContent={setContent} onSubmit={() => mutation.mutate()} />
    )}
  </Card.Body></Card>;
}

function InquiryAnswerForm({
  content,
  pending,
  error,
  onContent,
  onSubmit,
}: {
  content: string;
  pending: boolean;
  error: unknown;
  onContent: (value: string) => void;
  onSubmit: () => void;
}) {
  return <Form className="mt-2" onSubmit={(event) => {
    event.preventDefault();
    onSubmit();
  }}>
    <Form.Control as="textarea" rows={3} maxLength={CONTENT_BODY_MAX_LENGTH}
      value={content} onChange={(event) => onContent(event.target.value)}
      placeholder="스마트스토어에 등록할 답변" />
    <div className="d-flex justify-content-between align-items-center mt-1">
      <Form.Text>{contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}</Form.Text>
      <Button type="submit" size="sm" disabled={!content.trim() || pending}>답변 등록</Button>
    </div>
    <ErrorAlert error={error} />
  </Form>;
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
        <InquiryAnswerForm content={content} pending={mutation.isPending} error={mutation.error}
          onContent={setContent} onSubmit={() => mutation.mutate()} />
      )}
    </Card.Body>
  </Card>;
}
