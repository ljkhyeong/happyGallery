import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Container, Badge } from "react-bootstrap";
import { Link } from "react-router";
import { fetchNotice, publicNoticesQuery } from "@/features/notice/api";
import { ErrorAlert } from "@/shared/ui";
import { formatDate, formatDateTime } from "@/shared/lib";
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
  } = useLoaderBackedQuery({
    queryKey: noticeQueryKey,
    queryFn: () => fetchNotice(noticeId),
  }, initialNotice);

  return (
    <Container className="page-container notice-detail-page">
      <Link to="/notices" className="page-back-link">&larr; 공지사항</Link>

      <ErrorAlert error={error} />

      <article className="notice-article">
        <header>
          <h1>{notice.title}</h1>
          <p className="notice-meta">
            {notice.pinned && <Badge bg="dark" className="me-2">고정</Badge>}
            <time dateTime={notice.createdAt}>{formatDateTime(notice.createdAt)}</time> · 조회 {notice.viewCount}
          </p>
        </header>
        <div className="notice-content">{notice.content}</div>
      </article>

      <OtherNotices currentId={noticeId} />
    </Container>
  );
}

/** 상세 아래에서 최근 공지로 이어 간다. 전체 목록은 위의 공지사항 링크로 간다. */
function OtherNotices({ currentId }: { currentId: number }) {
  const { data: notices } = useQuery(publicNoticesQuery());
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
