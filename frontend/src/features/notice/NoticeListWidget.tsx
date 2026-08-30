import { Badge } from "react-bootstrap";
import { Link } from "react-router";
import { fetchNotices } from "./api";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, EmptyState, ErrorAlert } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { NoticeListResponse } from "@/generated/api/notice";

export function NoticeListWidget({ initialNotices }: { initialNotices: NoticeListResponse[] }) {
  const {
    data: notices,
    error,
    isLoading,
    query: { isFetching, refetch },
  } = useLoaderBackedQuery({
    queryKey: queryKeys.notices.all,
    queryFn: fetchNotices,
    staleTime: PUBLIC_DATA_STALE_TIME,
  }, initialNotices);

  const recent = notices?.slice(0, 5) ?? [];

  return (
    <section className="home-update-panel home-notice-panel" aria-labelledby="home-notice-title">
      <header className="home-update-heading">
        <p className="store-section-kicker">Notice</p>
        <h2 id="home-notice-title">공지사항</h2>
        <p>공방 이용 전 꼭 확인할 안내를 모았습니다.</p>
      </header>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert
        error={error}
        onRetry={() => void refetch()}
        retrying={isFetching}
      />
      {!isLoading && notices && recent.length === 0 && (
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
