import { Badge } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchNotices } from "./api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

export function NoticeListWidget() {
  const { data: notices, isLoading } = useQuery({
    queryKey: ["notices"],
    queryFn: fetchNotices,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });

  const recent = notices?.slice(0, 5) ?? [];

  return (
    <section className="store-section mb-5">
      <div className="store-section-header">
        <div>
          <p className="store-section-kicker mb-2">Notice</p>
          <h5 className="mb-1">공지사항</h5>
        </div>
      </div>
      {isLoading && <LoadingSpinner />}
      {!isLoading && recent.length === 0 && <EmptyState message="공지사항이 없습니다." />}
      {recent.length > 0 && (
        <div className="list-group list-group-flush">
          {recent.map((n) => (
            <Link
              key={n.id}
              to={`/notices/${n.id}`}
              className="list-group-item list-group-item-action d-flex justify-content-between align-items-center px-0"
            >
              <div className="d-flex align-items-center gap-2">
                {n.pinned && <Badge bg="dark" className="badge-sm">고정</Badge>}
                <span className="small">{n.title}</span>
              </div>
              <div className="d-flex align-items-center gap-3 text-muted-soft" style={{ fontSize: "0.8rem" }}>
                <span>조회 {n.viewCount}</span>
                <span>{formatDateTime(n.createdAt)}</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}
