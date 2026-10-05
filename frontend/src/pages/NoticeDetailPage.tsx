import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Container, Badge } from "react-bootstrap";
import { Link } from "react-router";
import { fetchNotice, fetchNotices } from "@/features/notice/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { formatDate, formatDateTime } from "@/shared/lib";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
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
    <Container className="page-container notice-detail-page">
      <Link to="/" className="page-back-link">&larr; 홈으로</Link>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />

      {notice && (
        <article className="notice-article">
          <header>
            <p className="store-section-kicker">
              Notice{notice.pinned && <Badge bg="dark" className="ms-2">고정</Badge>}
            </p>
            <h1>{notice.title}</h1>
            <p className="notice-meta">
              <time dateTime={notice.createdAt}>{formatDateTime(notice.createdAt)}</time> · 조회 {notice.viewCount}
            </p>
          </header>
          <div className="notice-content">{notice.content}</div>
        </article>
      )}

      <OtherNotices currentId={noticeId} />
    </Container>
  );
}

/** 공지 목록 화면이 따로 없으므로 상세 아래에서 다른 공지로 이어 간다. */
function OtherNotices({ currentId }: { currentId: number }) {
  const { data: notices } = useQuery({
    queryKey: queryKeys.notices.all,
    queryFn: fetchNotices,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });
  const others = notices?.filter((notice) => notice.id !== currentId).slice(0, 4) ?? [];
  if (others.length === 0) return null;

  return (
    <nav className="notice-others" aria-labelledby="notice-others-title">
      <h2 id="notice-others-title">다른 공지</h2>
      <ul>
        {others.map((notice) => (
          <li key={notice.id}>
            <Link to={`/notices/${notice.id}`}>{notice.title}</Link>
            <time dateTime={notice.createdAt}>{formatDate(notice.createdAt)}</time>
          </li>
        ))}
      </ul>
    </nav>
  );
}
