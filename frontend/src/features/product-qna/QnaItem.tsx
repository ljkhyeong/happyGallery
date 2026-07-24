import { useState } from "react";
import { Card, Badge, Button, Form, InputGroup } from "react-bootstrap";
import { useMutation } from "@tanstack/react-query";
import { ChevronDown, ChevronUp } from "lucide-react";
import { fetchProductQnaDetail, verifyQnaPassword } from "./api";
import { ErrorAlert } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { ProductQnaListItem, ProductQnaDetail } from "@/shared/types";

interface Props {
  item: ProductQnaListItem;
  productId: number;
}

export function QnaItem({ item, productId }: Props) {
  const [detail, setDetail] = useState<ProductQnaDetail | null>(null);
  const [password, setPassword] = useState("");

  const verifyMutation = useMutation({
    mutationFn: () => verifyQnaPassword(productId, item.id, password),
    onSuccess: (verifiedDetail) => {
      setDetail(verifiedDetail);
      setPassword("");
    },
  });

  const detailMutation = useMutation({
    mutationFn: () => fetchProductQnaDetail(productId, item.id),
    onSuccess: setDetail,
  });

  const isLocked = item.secret && !detail;
  const displayTitle = detail ? detail.title : item.title;
  const displayContent = detail?.content;
  const displayReply = detail?.replyContent;

  return (
    <Card className="mb-2">
      <Card.Body className="py-2 px-3">
        <div className="d-flex justify-content-between align-items-start">
          <div className="flex-grow-1">
            <div className="d-flex align-items-center gap-2 mb-1">
              {item.secret && <Badge bg="secondary" className="badge-sm">비밀글</Badge>}
              {item.hasReply && <Badge bg="info" className="badge-sm">답변완료</Badge>}
              <span className="fw-semibold small">{displayTitle}</span>
            </div>
            <div className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
              {item.authorName} | {formatDateTime(item.createdAt)}
            </div>
          </div>
        </div>

        {isLocked && (
          <div className="mt-2">
            <Form onSubmit={(e) => { e.preventDefault(); verifyMutation.mutate(); }}>
              <InputGroup size="sm">
                <Form.Control
                  type="password"
                  placeholder="비밀번호를 입력하세요"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <Button
                  variant="outline-secondary"
                  type="submit"
                  disabled={!password || verifyMutation.isPending}
                >
                  확인
                </Button>
              </InputGroup>
            </Form>
            <ErrorAlert error={verifyMutation.error} />
          </div>
        )}

        {!isLocked && displayContent && (
          <div className="mt-2 small">
            <div className="bg-light p-2 rounded">{displayContent}</div>
            {displayReply && (
              <div className="mt-2 p-2 rounded" style={{ background: "#f0f4ff" }}>
                <strong className="small">관리자 답변</strong>
                <div>{displayReply}</div>
              </div>
            )}
          </div>
        )}

        {!item.secret && (
          <div className="mt-2">
            <Button
              size="sm"
              variant="link"
              className="p-0"
              disabled={detailMutation.isPending}
              onClick={() => detail ? setDetail(null) : detailMutation.mutate()}
            >
              {detail ? <ChevronUp size={14} aria-hidden="true" /> : <ChevronDown size={14} aria-hidden="true" />}
              <span className="ms-1">
                {detail ? "내용 닫기" : detailMutation.isPending ? "불러오는 중..." : "내용 보기"}
              </span>
            </Button>
            <ErrorAlert error={detailMutation.error} />
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
