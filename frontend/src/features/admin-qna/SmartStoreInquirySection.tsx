import { useState } from "react";
import { Badge, Button, Card, Form } from "react-bootstrap";
import { useQueryClient } from "@tanstack/react-query";
import {
  answerChannelQna,
  answerCustomerInquiry,
  updateCustomerInquiryAnswer,
  fetchSmartStoreAnswerTemplate,
  fetchSmartStoreCustomerInquiries,
  fetchSmartStoreInquiries,
} from "./api";
import type {
  SmartStoreCustomerInquiryResponse,
  SmartStoreInquiryAnswerTemplateResponse,
  SmartStoreInquiryResponse,
} from "./api";
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
type InquiryType = "product" | "customer";
const dateInputFormat = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Seoul" });

function initialSearch() {
  const from = dateInputFormat.format(new Date(Date.now() - 29 * 86400000));
  const to = dateInputFormat.format(new Date());
  return { from, to, draftFrom: from, draftTo: to, unansweredOnly: true, page: 0 };
}

export function SmartStoreInquirySection({ token, onAuthError }: Props) {
  const [inquiryType, setInquiryType] = useState<InquiryType>("product");
  const [searches, setSearches] = useState(() => ({ product: initialSearch(), customer: initialSearch() }));
  const search = searches[inquiryType];
  const updateSearch = (change: Partial<typeof search>) => setSearches((previous) => ({
    ...previous, [inquiryType]: { ...previous[inquiryType], ...change },
  }));
  const productParams = {
    from: searches.product.from, to: searches.product.to,
    unansweredOnly: searches.product.unansweredOnly, page: searches.product.page, size: 50,
  };
  const customerParams = {
    from: searches.customer.from, to: searches.customer.to,
    unansweredOnly: searches.customer.unansweredOnly, page: searches.customer.page, size: 50,
  };
  const productQuery = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, "product", productParams],
    queryFn: () => fetchSmartStoreInquiries(token, productParams),
    enabled: inquiryType === "product",
    refetchInterval: 60_000,
  });
  const customerQuery = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, "customer", customerParams],
    queryFn: () => fetchSmartStoreCustomerInquiries(token, customerParams),
    enabled: inquiryType === "customer",
    refetchInterval: 60_000,
  });
  const templateQuery = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, "template"],
    queryFn: () => fetchSmartStoreAnswerTemplate(token),
    enabled: inquiryType === "product",
  });
  const activeQuery = inquiryType === "product" ? productQuery : customerQuery;

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
    <Form className="d-flex flex-wrap align-items-end gap-2 mb-3" onSubmit={(event) => {
      event.preventDefault();
      updateSearch({ from: search.draftFrom, to: search.draftTo, page: 0 });
    }}>
      <Form.Group controlId="smartstore-inquiry-from">
        <Form.Label className="small">문의 시작일</Form.Label>
        <Form.Control type="date" required value={search.draftFrom} max={search.draftTo}
          onChange={(event) => updateSearch({ draftFrom: event.target.value })} />
      </Form.Group>
      <Form.Group controlId="smartstore-inquiry-to">
        <Form.Label className="small">문의 종료일</Form.Label>
        <Form.Control type="date" required value={search.draftTo} min={search.draftFrom}
          onChange={(event) => updateSearch({ draftTo: event.target.value })} />
      </Form.Group>
      <Button type="submit" variant="outline-primary">문의 조회</Button>
    </Form>
    <Form.Check
      className="mb-3"
      type="switch"
      id="smartstore-inquiry-unanswered-only"
      label="미답변 문의만 보기"
      checked={search.unansweredOnly}
      onChange={(event) => updateSearch({ unansweredOnly: event.target.checked, page: 0 })}
    />
    {inquiryType === "product" && templateQuery.error && (
      <ErrorAlert error={templateQuery.error}
        onRetry={() => { void templateQuery.refetch(); }} retrying={templateQuery.isFetching} />
    )}
    {activeQuery.isLoading && <LoadingSpinner />}
    <ErrorAlert error={activeQuery.error}
      onRetry={() => { void activeQuery.refetch(); }} retrying={activeQuery.isFetching} />
    <p className="small text-muted-soft">조회 기간 {search.from} ~ {search.to} · 50건씩 표시</p>
    {activeQuery.data && (activeQuery.data.content.length === 0 ? (
      <EmptyState message={search.unansweredOnly
        ? "답변을 기다리는 스마트스토어 문의가 없습니다."
        : "선택한 기간의 스마트스토어 문의가 없습니다."} />
    ) : inquiryType === "product"
      ? productQuery.data?.content.map((inquiry) => (
        <SmartStoreInquiryCard key={inquiry.questionId} inquiry={inquiry}
          template={templateQuery.data} token={token} onAuthError={onAuthError} />
      ))
      : customerQuery.data?.content.map((inquiry) => (
        <SmartStoreCustomerInquiryCard key={inquiry.inquiryNo} inquiry={inquiry}
          token={token} onAuthError={onAuthError} />
      )))}
    {activeQuery.data && (
      <div className="d-flex flex-wrap align-items-center gap-2 mt-3">
        <Button size="sm" variant="outline-secondary" disabled={search.page === 0 || activeQuery.isFetching}
          onClick={() => updateSearch({ page: search.page - 1 })}>이전 페이지</Button>
        <span className="small">{search.page + 1} / {Math.max(1, activeQuery.data.totalPages)}페이지 · 총 {activeQuery.data.totalCount}건</span>
        <Button size="sm" variant="outline-secondary"
          disabled={search.page + 1 >= activeQuery.data.totalPages || activeQuery.isFetching}
          onClick={() => updateSearch({ page: search.page + 1 })}>다음 페이지</Button>
      </div>
    )}
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
  const [editingAnswerId, setEditingAnswerId] = useState<number | null>(null);
  const editing = editingAnswerId !== null;
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => editingAnswerId !== null
      ? updateCustomerInquiryAnswer(inquiry.inquiryNo, editingAnswerId, content, token)
      : answerCustomerInquiry(inquiry.inquiryNo, content, token),
    onSuccess: async () => {
      setContent("");
      setEditingAnswerId(null);
      toast.show("스마트스토어 주문·배송 문의 답변을 저장했습니다.");
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
    {inquiry.answered && !editing ? (
      <div className="small rounded p-2 mt-2" style={{ background: "#f0f4ff" }}>
        <strong>답변:</strong> {inquiry.answerContent}
        {inquiry.answerContentId != null && (
          <Button type="button" size="sm" variant="outline-secondary" className="ms-2"
            onClick={() => {
              setContent(inquiry.answerContent ?? "");
              mutation.reset();
              setEditingAnswerId(inquiry.answerContentId);
            }}>답변 수정</Button>
        )}
      </div>
    ) : (
      <InquiryAnswerForm content={content} pending={mutation.isPending} error={mutation.error}
        submitLabel={editing ? "수정 저장" : "답변 등록"}
        onCancel={editing ? () => { setEditingAnswerId(null); mutation.reset(); } : undefined}
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
  template,
  submitLabel = "답변 등록",
  onCancel,
}: {
  content: string;
  pending: boolean;
  error: unknown;
  onContent: (value: string) => void;
  onSubmit: () => void;
  template?: SmartStoreInquiryAnswerTemplateResponse;
  submitLabel?: string;
  onCancel?: () => void;
}) {
  return <Form className="mt-2" onSubmit={(event) => {
    event.preventDefault();
    onSubmit();
  }}>
    <Form.Control as="textarea" rows={3} maxLength={CONTENT_BODY_MAX_LENGTH}
      disabled={pending}
      value={content} onChange={(event) => onContent(event.target.value)}
      placeholder="스마트스토어에 등록할 답변" />
    {template && (
      <Button className="mt-2" type="button" size="sm" variant="outline-secondary"
        disabled={pending}
        onClick={() => onContent(template.content)}>
        {template.subject} 적용
      </Button>
    )}
    <div className="d-flex justify-content-between align-items-center mt-1">
      <Form.Text>{contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}</Form.Text>
      <div className="d-flex gap-2">
        {onCancel && <Button type="button" size="sm" variant="outline-secondary" disabled={pending} onClick={onCancel}>취소</Button>}
        <Button type="submit" size="sm" disabled={!content.trim() || pending}>{submitLabel}</Button>
      </div>
    </div>
    <ErrorAlert error={error} />
  </Form>;
}

function SmartStoreInquiryCard({
  inquiry,
  template,
  token,
  onAuthError,
}: {
  inquiry: SmartStoreInquiryResponse;
  template?: SmartStoreInquiryAnswerTemplateResponse;
  token: string;
  onAuthError: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [content, setContent] = useState("");
  const [editing, setEditing] = useState(false);
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: () => answerChannelQna(inquiry.questionId, content, token),
    onSuccess: async () => {
      setContent("");
      setEditing(false);
      toast.show("스마트스토어 상품 문의 답변을 저장했습니다.");
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
      {inquiry.answered && !editing ? (
        <div className="small rounded p-2 mt-2" style={{ background: "#f0f4ff" }}>
          <strong>답변:</strong> {inquiry.answer}
          <Button type="button" size="sm" variant="outline-secondary" className="ms-2"
            onClick={() => {
              setContent(inquiry.answer ?? "");
              mutation.reset();
              setEditing(true);
            }}>답변 수정</Button>
        </div>
      ) : (
        <InquiryAnswerForm content={content} pending={mutation.isPending} error={mutation.error}
          submitLabel={editing ? "수정 저장" : "답변 등록"}
          onCancel={editing ? () => { setEditing(false); mutation.reset(); } : undefined}
          template={template} onContent={setContent} onSubmit={() => mutation.mutate()} />
      )}
    </Card.Body>
  </Card>;
}
