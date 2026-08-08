import { useState } from "react";
import { Form, Button, Card } from "react-bootstrap";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createQna } from "./api";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import {
  CONTENT_BODY_MAX_LENGTH,
  CONTENT_TITLE_MAX_LENGTH,
  contentLengthLabel,
} from "@/shared/validation/contentText";

interface Props {
  productId: number;
}

export function QnaCreateForm({ productId }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [secret, setSecret] = useState(false);
  const [open, setOpen] = useState(false);
  const titleControlId = `product-qna-title-${productId}`;
  const titleCountId = `${titleControlId}-count`;
  const contentControlId = `product-qna-content-${productId}`;
  const contentCountId = `${contentControlId}-count`;

  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => createQna(productId, {
          title,
          content,
          secret,
        }),
      () => {
        toast.show("Q&A가 등록되었습니다.");
        setTitle("");
        setContent("");
        setSecret(false);
        setOpen(false);
        void queryClient.invalidateQueries({
          queryKey: queryKeys.productQna.byProduct(productId),
        });
        void queryClient.invalidateQueries({
          queryKey: queryKeys.member.productQna.byProduct(productId),
        });
      },
    ),
  });

  if (!open) {
    return (
      <Button variant="outline-primary" size="sm" onClick={() => setOpen(true)}>
        질문 작성
      </Button>
    );
  }

  const canSubmit =
    title.trim().length > 0 &&
    content.trim().length > 0;

  return (
    <Card className="mb-3">
      <Card.Body>
        <h6 className="mb-3">질문 작성</h6>
        <Form onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}>
          <Form.Group className="mb-2" controlId={titleControlId}>
            <Form.Label>제목</Form.Label>
            <Form.Control
              placeholder="제목"
              maxLength={CONTENT_TITLE_MAX_LENGTH}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              aria-describedby={titleCountId}
            />
            <Form.Text id={titleCountId} className="text-muted d-block text-end">
              {contentLengthLabel(title, CONTENT_TITLE_MAX_LENGTH)}
            </Form.Text>
          </Form.Group>
          <Form.Group className="mb-2" controlId={contentControlId}>
            <Form.Label>내용</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              placeholder="질문 내용을 입력하세요"
              maxLength={CONTENT_BODY_MAX_LENGTH}
              value={content}
              onChange={(e) => setContent(e.target.value)}
              aria-describedby={contentCountId}
            />
            <Form.Text id={contentCountId} className="text-muted d-block text-end">
              {contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}
            </Form.Text>
          </Form.Group>
          <Form.Check
            type="checkbox"
            id="qna-secret"
            label="비밀글"
            checked={secret}
            onChange={(e) => setSecret(e.target.checked)}
            className="mb-2"
          />
          {secret && (
            <Form.Text className="text-muted d-block mb-2">
              비밀글은 작성자와 관리자만 볼 수 있습니다.
            </Form.Text>
          )}
          <ErrorAlert error={mutation.error} />
          <div className="d-flex gap-2">
            <Button type="submit" size="sm" disabled={!canSubmit || mutation.isPending}>
              {mutation.isPending ? "등록 중..." : "등록"}
            </Button>
            <Button variant="outline-secondary" size="sm" onClick={() => setOpen(false)}>
              취소
            </Button>
          </div>
        </Form>
      </Card.Body>
    </Card>
  );
}
