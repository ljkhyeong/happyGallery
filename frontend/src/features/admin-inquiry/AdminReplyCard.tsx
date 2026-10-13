import type { ReactNode } from "react";
import { Badge, Button, Card, Form, InputGroup } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import {
  CONTENT_BODY_MAX_LENGTH,
  contentLengthLabel,
} from "@/shared/validation/contentText";

interface Props {
  title: string;
  content: string;
  replyContent: string | null;
  /** 작성자·시각 등 제목 아래 한 줄 정보 */
  meta: string;
  /** 답변 상태 배지 앞에 둘 배지(비밀글 등) */
  badges?: ReactNode;
  /** 화면에 보이지 않는 답변 입력 칸 이름. 같은 화면의 다른 답변 칸과 구분한다. */
  replyLabel: string;
  replyControlId: string;
  replyText: string;
  onReplyTextChange: (value: string) => void;
  onSubmit: () => void;
  pending: boolean;
  error: unknown;
}

/** 1:1 문의·상품 문의의 질문 내용과 답변(또는 답변 입력)을 보여 준다. 답변 저장은 각 영역이 맡는다. */
export function AdminReplyCard({
  title,
  content,
  replyContent,
  meta,
  badges,
  replyLabel,
  replyControlId,
  replyText,
  onReplyTextChange,
  onSubmit,
  pending,
  error,
}: Props) {
  const replyCountId = `${replyControlId}-count`;

  return (
    <Card className="mb-2">
      <Card.Body className="py-2 px-3">
        <div className="d-flex justify-content-between align-items-start">
          <div>
            <div className="d-flex align-items-center gap-2 mb-1">
              {badges}
              {replyContent ? (
                <Badge bg="info" className="badge-sm">답변 완료</Badge>
              ) : (
                <Badge bg="warning" className="badge-sm">답변 대기</Badge>
              )}
              <span className="fw-semibold small">{title}</span>
            </div>
            <div className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
              {meta}
            </div>
          </div>
        </div>
        <div className="mt-2 small bg-light p-2 rounded">{content}</div>

        {replyContent && (
          <div className="mt-2 p-2 rounded small" style={{ background: "#f0f4ff" }}>
            <strong>답변:</strong> {replyContent}
          </div>
        )}

        {!replyContent && (
          <Form
            className="mt-2"
            onSubmit={(e) => {
              e.preventDefault();
              onSubmit();
            }}
          >
            <Form.Group controlId={replyControlId}>
              <Form.Label className="visually-hidden">{replyLabel}</Form.Label>
              <InputGroup size="sm">
                <Form.Control
                  as="textarea"
                  rows={3}
                  placeholder="답변을 입력하세요"
                  maxLength={CONTENT_BODY_MAX_LENGTH}
                  value={replyText}
                  onChange={(e) => onReplyTextChange(e.target.value)}
                  aria-describedby={replyCountId}
                />
                <Button
                  type="submit"
                  variant="primary"
                  disabled={!replyText.trim() || pending}
                >
                  답변
                </Button>
              </InputGroup>
              <Form.Text id={replyCountId} className="text-muted d-block text-end">
                {contentLengthLabel(replyText, CONTENT_BODY_MAX_LENGTH)}
              </Form.Text>
            </Form.Group>
            <ErrorAlert error={error} />
          </Form>
        )}
      </Card.Body>
    </Card>
  );
}
