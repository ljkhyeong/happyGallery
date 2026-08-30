import { useState } from "react";
import { Container, Card, Form, Button } from "react-bootstrap";
import { useNavigate, Link } from "react-router";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createInquiry } from "@/features/my-inquiry/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, useToast } from "@/shared/ui";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
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

  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => createInquiry({ title, content }),
      async (_, requireCurrent) => {
        await queryClient.invalidateQueries({ queryKey: queryKeys.member.inquiries });
        requireCurrent();
        toast.show("문의가 등록되었습니다.");
        navigate("/my/inquiries");
      },
    ),
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
          <Form onSubmit={(e) => { e.preventDefault(); mutation.mutate(); }}>
            <Form.Group className="mb-3" controlId="my-inquiry-title">
              <Form.Label>제목</Form.Label>
              <Form.Control
                placeholder="문의 제목을 입력하세요"
                maxLength={CONTENT_TITLE_MAX_LENGTH}
                value={title}
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
              <Button variant="outline-secondary" onClick={() => navigate("/my/inquiries")}>
                취소
              </Button>
            </div>
          </Form>
        </Card.Body>
      </Card>

      <div className="mt-3">
        <Link to="/my/inquiries" className="text-decoration-none">&larr; 내 문의 목록</Link>
      </div>
    </Container>
  );
}
