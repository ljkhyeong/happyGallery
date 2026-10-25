import { Badge } from "react-bootstrap";
import { Link } from "react-router";
import { publicNoticesQuery } from "./api";
import { useLoaderBackedQuery } from "@/shared/api";
import { EmptyState, ErrorAlert } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { NoticeListResponse } from "@/generated/api/notice";

export function NoticeListWidget({ initialNotices }: { initialNotices: NoticeListResponse[] }) {
  const {
    data: notices,
    error,
    query: { isFetching, refetch },
  } = useLoaderBackedQuery(publicNoticesQuery(), initialNotices);

  const recent = notices.slice(0, 5);

  return (
    <section className="home-update-panel" aria-labelledby="home-notice-title">
      <header className="home-update-heading">
        <h2 id="home-notice-title">공지사항</h2>
        <Link to="/notices" className="gallery-section-more">전체 공지 보기</Link>
      </header>
      <ErrorAlert
        error={error}
        onRetry={() => void refetch()}
        retrying={isFetching}
      />
      {recent.length === 0 && (
        <EmptyState message="공지사항이 없습니다." />
      )}
      {recent.length > 0 && (
        <div className="home-notice-list">
          {recent.map((n) => (
            <Link
              key={n.id}
              to={`/notices/${n.id}`}
              className="home-notice-row"
            >
              <div className="home-notice-title">
                {n.pinned && <Badge bg="dark" className="badge-sm">고정</Badge>}
                <strong>{n.title}</strong>
              </div>
              <div className="home-notice-meta">
                <span>조회 {n.viewCount}</span>
                <time dateTime={n.createdAt}>{formatDateTime(n.createdAt)}</time>
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}
