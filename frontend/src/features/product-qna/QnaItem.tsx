import { useState } from "react";
import { Card, Badge, Button } from "react-bootstrap";
import { useQuery } from "@tanstack/react-query";
import { ChevronDown, ChevronUp } from "lucide-react";
import { fetchMyProductQnaDetail, fetchProductQnaDetail } from "./api";
import { queryKeys } from "@/shared/api";
import { ErrorAlert } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { ProductQnaListItem } from "@/shared/types";

interface Props {
  item: ProductQnaListItem;
  productId: number;
  owned?: boolean;
}

export function QnaItem({ item, productId, owned }: Props) {
  const [expanded, setExpanded] = useState(false);

  const detailQuery = useQuery({
    queryKey: item.secret
      ? queryKeys.member.productQna.detail(productId, item.id)
      : queryKeys.productQna.detail(productId, item.id),
    queryFn: ({ signal }) => item.secret
      ? fetchMyProductQnaDetail(productId, item.id, signal)
      : fetchProductQnaDetail(productId, item.id, signal),
    enabled: expanded && (!item.secret || owned === true),
  });

  const detail = expanded ? detailQuery.data : undefined;
  const isLocked = item.secret && !detail;
  const displayTitle = detail ? detail.title : item.title;
  const displayContent = detail?.content;
  const displayReply = detail?.replyContent;
  const openDetail = () => {
    if (expanded) {
      void detailQuery.refetch();
      return;
    }
    setExpanded(true);
  };
  const toggleDetail = () => {
    if (detail) {
      setExpanded(false);
      return;
    }
    openDetail();
  };

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
            {owned === true && (
              <>
                <Button
                  size="sm"
                  variant="link"
                  className="p-0"
                  disabled={detailQuery.isFetching}
                  onClick={openDetail}
                >
                  <ChevronDown size={14} aria-hidden="true" />
                  <span className="ms-1">
                    {detailQuery.isFetching ? "불러오는 중..." : "작성자 전용 내용 보기"}
                  </span>
                </Button>
                <ErrorAlert error={detailQuery.error} />
              </>
            )}
            {owned === false && (
              <span className="text-muted-soft small">작성자만 볼 수 있는 비밀글입니다.</span>
            )}
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

        {(!item.secret || detail) && (
          <div className="mt-2">
            <Button
              size="sm"
              variant="link"
              className="p-0"
              disabled={detailQuery.isFetching}
              onClick={toggleDetail}
            >
              {detail ? <ChevronUp size={14} aria-hidden="true" /> : <ChevronDown size={14} aria-hidden="true" />}
              <span className="ms-1">
                {detail ? "내용 닫기" : detailQuery.isFetching ? "불러오는 중..." : "내용 보기"}
              </span>
            </Button>
            <ErrorAlert error={detailQuery.error} />
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
