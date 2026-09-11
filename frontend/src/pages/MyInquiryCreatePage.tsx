import { useId, useRef, useState } from "react";
import { Container, Card, Form, Button, Modal } from "react-bootstrap";
import { useNavigate, useBlocker, useBeforeUnload, Link } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createInquiry } from "@/features/my-inquiry/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, useToast } from "@/shared/ui";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import {
  captureCustomerSession,
  queryKeys,
  requireCurrentCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import {
  CONTENT_BODY_MAX_LENGTH,
  CONTENT_TITLE_MAX_LENGTH,
  contentLengthLabel,
} from "@/shared/validation/contentText";

export function MyInquiryCreatePage() {
  const { sessionVersion } = useCustomerAuth();
  return <MyInquiryCreateContent key={sessionVersion} />;
}

function MyInquiryCreateContent() {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const loginHref = buildAuthPageHref("/login", { redirectTo: "/my/inquiries/new" });

  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const submission = useRef({ pending: false, completed: false });
  const leaveTitleId = useId();
  const shouldConfirmLeave = () => isAuthenticated && !submission.current.completed
    && (title.length > 0 || content.length > 0 || submission.current.pending);
  const blocker = useBlocker(shouldConfirmLeave);
  useBeforeUnload((event) => {
    if (!shouldConfirmLeave()) return;
    event.preventDefault();
    event.returnValue = "";
  });

  const mutation = useMutation({
    mutationFn: (customerSession: CustomerSessionSnapshot) => runForCurrentCustomer(
      () => {
        requireCurrentCustomerSession(customerSession);
        return createInquiry({ title, content });
      },
      async (_, requireCurrent) => {
        submission.current.completed = true;
        await queryClient.invalidateQueries({ queryKey: queryKeys.member.inquiries });
        requireCurrent();
        toast.show("문의가 등록되었습니다.");
        navigate("/my/inquiries");
      },
    ),
    onSettled: () => { submission.current.pending = false; },
  });

  if (authLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container">
        <Card className="text-center p-4">
          <p>로그인이 필요합니다.</p>
          <Link to={loginHref}>로그인</Link>
        </Card>
      </Container>
    );
  }

  const canSubmit = title.trim().length > 0 && content.trim().length > 0;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <h4 className="mb-3">1:1 문의 작성</h4>

      <Card>
        <Card.Body>
          <Form onSubmit={(e) => {
            e.preventDefault();
            if (!canSubmit || submission.current.pending) return;
            submission.current.pending = true;
            mutation.mutate(captureCustomerSession());
          }}>
            <Form.Group className="mb-3" controlId="my-inquiry-title">
              <Form.Label>제목</Form.Label>
              <Form.Control
                placeholder="문의 제목을 입력하세요"
                maxLength={CONTENT_TITLE_MAX_LENGTH}
                value={title}
                disabled={mutation.isPending}
                onChange={(e) => setTitle(e.target.value)}
                aria-describedby="my-inquiry-title-count"
              />
              <Form.Text id="my-inquiry-title-count" className="text-muted d-block text-end">
                {contentLengthLabel(title, CONTENT_TITLE_MAX_LENGTH)}
              </Form.Text>
            </Form.Group>
            <Form.Group className="mb-3" controlId="my-inquiry-content">
              <Form.Label>내용</Form.Label>
              <Form.Control
                as="textarea"
                rows={5}
                placeholder="문의 내용을 입력하세요"
                maxLength={CONTENT_BODY_MAX_LENGTH}
                value={content}
                disabled={mutation.isPending}
                onChange={(e) => setContent(e.target.value)}
                aria-describedby="my-inquiry-content-count"
              />
              <Form.Text id="my-inquiry-content-count" className="text-muted d-block text-end">
                {contentLengthLabel(content, CONTENT_BODY_MAX_LENGTH)}
              </Form.Text>
            </Form.Group>

            <ErrorAlert error={mutation.error} />

            <div className="d-flex gap-2">
              <Button type="submit" disabled={!canSubmit || mutation.isPending}>
                {mutation.isPending ? "등록 중..." : "등록"}
              </Button>
              <Button variant="outline-secondary" disabled={mutation.isPending} onClick={() => navigate("/my/inquiries")}>
                취소
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>

      <div className="mt-3">
        <Link to="/my/inquiries" className="text-decoration-none">&larr; 내 문의 목록</Link>
      </div>

      <Modal show={blocker.state === "blocked"} onHide={() => blocker.reset?.()}
        aria-labelledby={leaveTitleId} centered>
        <Modal.Header closeButton>
          <Modal.Title id={leaveTitleId} className="fs-6">
            {mutation.isPending ? "문의 등록 중" : "문의 작성을 그만둘까요?"}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {mutation.isPending ? "문의 등록 중입니다. 잠시만 기다려 주세요." : "작성한 내용이 저장되지 않습니다."}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={() => blocker.reset?.()}>
            {mutation.isPending ? "돌아가기" : "계속 작성"}
          </Button>
          <Button variant="danger" disabled={mutation.isPending} onClick={() => blocker.proceed?.()}>
            나가기
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}
