import { Container, Badge } from "react-bootstrap";
import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchNotice } from "@/features/notice/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

export function NoticeDetailPage() {
  const { id } = useParams<{ id: string }>();
  const noticeId = Number(id);

  const { data: notice, isLoading, error } = useQuery({
    queryKey: ["notices", noticeId],
    queryFn: () => fetchNotice(noticeId),
    enabled: !isNaN(noticeId),
  });

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <Link to="/" className="text-decoration-none small text-muted-soft d-inline-block mb-3">
        &larr; 홈으로
      </Link>

      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />

      {notice && (
        <>
          <div className="mb-3">
            <div className="d-flex align-items-center gap-2 mb-2">
              {notice.pinned && <Badge bg="dark" className="badge-sm">고정</Badge>}
              <h4 className="mb-0">{notice.title}</h4>
            </div>
            <div className="text-muted-soft small">
              {formatDateTime(notice.createdAt)} · 조회 {notice.viewCount}
            </div>
          </div>
          <hr />
          <div className="notice-content" style={{ whiteSpace: "pre-wrap", lineHeight: 1.8 }}>
            {notice.content}
          </div>
        </>
      )}
    </Container>
  );
}
