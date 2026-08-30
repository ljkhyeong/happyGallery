import { useMemo } from "react";
import { Container, Badge } from "react-bootstrap";
import { Link } from "react-router";
import { fetchNotice } from "@/features/notice/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import type { NoticeDetailResponse } from "@/generated/api/notice";

export function NoticeDetailPage({ initialNotice }: { initialNotice: NoticeDetailResponse }) {
  const noticeId = initialNotice.id;
  const noticeQueryKey = useMemo(
    () => queryKeys.notices.detail(noticeId),
    [noticeId],
  );

  const {
    data: notice,
    error,
    isLoading,
  } = useLoaderBackedQuery({
    queryKey: noticeQueryKey,
    queryFn: () => fetchNotice(noticeId),
  }, initialNotice);

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <Link to="/" className="text-decoration-none small text-muted-soft d-inline-block mb-3">
        &larr; 홈으로
      </Link>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />

      {notice && (
        <article>
          <div className="mb-3">
            <div className="d-flex align-items-center gap-2 mb-2">
              {notice.pinned && <Badge bg="dark" className="badge-sm">고정</Badge>}
              <h1 className="h4 mb-0">{notice.title}</h1>
            </div>
            <div className="text-muted-soft small">
              {formatDateTime(notice.createdAt)} · 조회 {notice.viewCount}
            </div>
          </div>
          <hr />
          <div className="notice-content" style={{ whiteSpace: "pre-wrap", lineHeight: 1.8 }}>
            {notice.content}
          </div>
        </article>
      )}
    </Container>
  );
}
