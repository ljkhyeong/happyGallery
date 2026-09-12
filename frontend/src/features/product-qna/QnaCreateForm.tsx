import { useEffect, useId, useRef, useState } from "react";
import { useBeforeUnload, useBlocker } from "react-router";
import { Form, Button, Card, Modal } from "react-bootstrap";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createQna } from "./api";
import {
  captureCustomerSession, queryKeys, requireCurrentCustomerSession, runForCustomerSession,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import type { CreateQnaRequest } from "@/generated/api/productQna";
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
  const [confirmDiscard, setConfirmDiscard] = useState(false);
  const submitting = useRef(false);
  const leaveTitleId = useId();
  const hasDraft = title.length > 0 || content.length > 0 || secret;
  const shouldConfirmLeave = () => hasDraft || submitting.current;
  const blocker = useBlocker(shouldConfirmLeave);
  useBeforeUnload((event) => {
    if (!shouldConfirmLeave()) return;
    event.preventDefault();
    event.returnValue = "";
  });
  const titleControlId = `product-qna-title-${productId}`;
  const titleCountId = `${titleControlId}-count`;
  const contentControlId = `product-qna-content-${productId}`;
  const contentCountId = `${contentControlId}-count`;

  const clearDraft = () => {
    setTitle("");
    setContent("");
    setSecret(false);
    setOpen(false);
    setConfirmDiscard(false);
  };
  const mutation = useMutation({
    mutationFn: ({ request, customerSession }: { request: CreateQnaRequest; customerSession: CustomerSessionSnapshot }) => runForCustomerSession(
      customerSession,
      async () => {
        await createQna(productId, request);
        requireCurrentCustomerSession(customerSession);
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: queryKeys.productQna.byProduct(productId) }),
          queryClient.invalidateQueries({ queryKey: queryKeys.member.productQna.byProduct(productId) }),
        ]);
        requireCurrentCustomerSession(customerSession);
        toast.show("상품 문의를 등록했습니다.");
        clearDraft();
      },
    ),
    onSettled: () => { submitting.current = false; },
  });

  useEffect(() => {
    if (blocker.state === "blocked" && !hasDraft && !mutation.isPending) blocker.proceed();
  }, [blocker, hasDraft, mutation.isPending]);

  const canSubmit =
    title.trim().length > 0 &&
    content.trim().length > 0;

  const keepWriting = () => {
    setConfirmDiscard(false);
    blocker.reset?.();
  };

  return <>
    {!open ? (
      <Button variant="outline-primary" size="sm" onClick={() => setOpen(true)}>문의 작성</Button>
    ) : (
      <Card className="mb-0 w-100">
        <Card.Body>
          <h6 className="mb-3">문의 작성</h6>
          <Form onSubmit={(event) => {
            event.preventDefault();
            if (!canSubmit || submitting.current) return;
            submitting.current = true;
            mutation.mutate({ request: { title, content, secret }, customerSession: captureCustomerSession() });
          }}>
            <fieldset disabled={mutation.isPending}>
              <Form.Group className="mb-2" controlId={titleControlId}>
                <Form.Label>제목</Form.Label>
                <Form.Control
                  placeholder="제목"
                  required
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
                  placeholder="문의 내용을 입력하세요"
                  required
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
                id={`qna-secret-${productId}`}
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
                <Button type="button" variant="outline-secondary" size="sm" onClick={() => {
                  if (hasDraft) setConfirmDiscard(true);
                  else { clearDraft(); mutation.reset(); }
                }}>
                  취소
                </Button>
              </div>
            </fieldset>
          </Form>
        </Card.Body>
      </Card>
    )}
    <Modal show={confirmDiscard || blocker.state === "blocked"} onHide={keepWriting} centered aria-labelledby={leaveTitleId}>
      <Modal.Header closeButton><Modal.Title id={leaveTitleId} className="fs-6">
        {mutation.isPending ? "상품 문의 등록 중" : "문의 작성을 그만둘까요?"}
      </Modal.Title></Modal.Header>
      <Modal.Body>{mutation.isPending ? "문의 등록 중입니다. 잠시만 기다려 주세요." : "작성한 제목과 내용이 삭제됩니다."}</Modal.Body>
      <Modal.Footer>
        <Button variant="outline-secondary" onClick={keepWriting}>{mutation.isPending ? "돌아가기" : "계속 작성"}</Button>
        <Button variant="danger" disabled={mutation.isPending} onClick={() => {
          clearDraft();
          mutation.reset();
          blocker.proceed?.();
        }}>{blocker.state === "blocked" ? "내용 버리고 이동" : "내용 버리기"}</Button>
      </Modal.Footer>
    </Modal>
  </>;
}
