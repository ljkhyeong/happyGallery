import { Badge, Container } from "react-bootstrap";
import { Link } from "react-router";
import { publicNoticesQuery } from "@/features/notice/api";
import { useLoaderBackedQuery } from "@/shared/api";
import { formatDate } from "@/shared/lib";
import { EmptyState, ErrorAlert, PageHeader } from "@/shared/ui";
import type { NoticeListResponse } from "@/generated/api/notice";

export function NoticesPage({ initialNotices }: { initialNotices: NoticeListResponse[] }) {
  const {
    data: notices,
    error,
    query: { isFetching, refetch },
  } = useLoaderBackedQuery(publicNoticesQuery(), initialNotices);

  return (
    <Container className="page-container notice-list-page">
      <PageHeader title="공지사항" />
      <ErrorAlert error={error} onRetry={() => void refetch()} retrying={isFetching} />
      {notices.length === 0 && <EmptyState message="공지사항이 없습니다." />}
      {notices.length > 0 && (
        <ul className="notice-list">
          {notices.map((notice) => (
            <li key={notice.id}>
              <Link to={`/notices/${notice.id}`}>
                {notice.pinned && <Badge bg="dark">고정</Badge>}
                <span>{notice.title}</span>
              </Link>
              <time dateTime={notice.createdAt}>{formatDate(notice.createdAt)}</time>
            </li>
          ))}
        </ul>
      )}
    </Container>
  );
}
