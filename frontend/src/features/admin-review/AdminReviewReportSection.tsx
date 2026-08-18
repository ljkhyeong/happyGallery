import { useRef, useState } from "react";
import { Badge, Button, Card, Form } from "react-bootstrap";
import { queryKeys } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { AdminReviewReportDetail } from "./AdminReviewReportDetail";
import {
  fetchAdminReviewReport,
  fetchAdminReviewReports,
  type AdminReviewReportSummaryResponse,
  type ReviewReportStatus,
} from "./api";

const REPORT_REASON_LABELS: Record<string, string> = {
  SPAM: "광고·도배",
  ABUSIVE: "욕설·비방",
  PRIVACY: "개인정보 노출",
  FALSE_INFORMATION: "허위 정보",
  OTHER: "기타",
};

export function AdminReviewReportSection({
  adminKey,
  onAuthError,
  onOpenReview,
}: {
  adminKey: string;
  onAuthError: () => void;
  onOpenReview: (reviewId: number) => void;
}) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  const [status, setStatus] = useState<ReviewReportStatus | "">("PENDING");
  const { cursor, hasPreviousPage, showNextPage, showPreviousPage, resetCursor } = useCursorHistory();
  const query = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.reviews.reports.page(status || undefined, cursor),
    queryFn: ({ signal }) => fetchAdminReviewReports(
      adminKey,
      { status: status || undefined, cursor },
      signal,
    ),
  });

  return (
    <section aria-labelledby="admin-review-report-heading">
      <div className="admin-review-report-header">
        <div>
          <h6
            ref={headingRef}
            id="admin-review-report-heading"
            className="mb-1"
            tabIndex={-1}
          >
            후기 신고
          </h6>
          <p className="text-muted-soft small mb-0">신고 판단과 후기 비공개 처리는 별도로 진행합니다.</p>
        </div>
        <Form.Select
          size="sm"
          aria-label="신고 처리 상태"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as ReviewReportStatus | "");
            resetCursor();
          }}
        >
          <option value="">전체</option>
          <option value="PENDING">처리 대기</option>
          <option value="ACCEPTED">위반 인정</option>
          <option value="REJECTED">신고 반려</option>
        </Form.Select>
      </div>
      {query.isLoading && <LoadingSpinner text="후기 신고를 불러오는 중입니다" />}
      <ErrorAlert
        error={query.error}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
      {query.data?.content.length === 0 && <EmptyState message="조건에 맞는 후기 신고가 없습니다." />}
      <div className="admin-review-list">
        {query.data?.content.map((report) => (
          <AdminReviewReportCard
            key={report.id}
            report={report}
            adminKey={adminKey}
            onAuthError={onAuthError}
            onOpenReview={onOpenReview}
            onDecisionComplete={() => headingRef.current?.focus()}
          />
        ))}
      </div>
      {(hasPreviousPage || query.data?.hasMore) && (
        <div className="d-flex justify-content-center gap-2 mt-3">
          <Button type="button" size="sm" variant="outline-secondary" disabled={!hasPreviousPage || query.isFetching} onClick={showPreviousPage}>
            이전
          </Button>
          <Button type="button" size="sm" variant="outline-primary" disabled={!query.data?.hasMore || query.isFetching} onClick={() => showNextPage(query.data?.nextCursor)}>
            다음
          </Button>
        </div>
      )}
    </section>
  );
}

function AdminReviewReportCard({
  report: summary,
  adminKey,
  onAuthError,
  onOpenReview,
  onDecisionComplete,
}: {
  report: AdminReviewReportSummaryResponse;
  adminKey: string;
  onAuthError: () => void;
  onOpenReview: (reviewId: number) => void;
  onDecisionComplete: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const detailQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.reviews.reports.detail(summary.id),
    queryFn: ({ signal }) => fetchAdminReviewReport(adminKey, summary.id, signal),
    enabled: expanded,
  });

  return (
    <Card className="admin-review-card admin-review-report-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <Badge bg={summary.status === "PENDING" ? "warning" : summary.status === "ACCEPTED" ? "danger" : "secondary"}>
              {summary.status === "PENDING" ? "처리 대기" : summary.status === "ACCEPTED" ? "위반 인정" : "신고 반려"}
            </Badge>
            <strong>{REPORT_REASON_LABELS[summary.reason]}</strong>
          </div>
          <small className="text-muted-soft">{formatDateTime(summary.createdAt)}</small>
        </div>
        <div className="small text-muted-soft mt-3">
          후기 #{summary.reviewId} · 신고 당시 {summary.snapshotStatus === "PUBLISHED" ? "공개" : "비공개"}
        </div>
        <Button
          type="button"
          size="sm"
          variant="outline-secondary"
          className="mt-3"
          aria-expanded={expanded}
          onClick={() => setExpanded((current) => !current)}
        >
          {expanded ? "상세 닫기" : "신고 상세 검토"}
        </Button>
        {expanded && detailQuery.isLoading && <LoadingSpinner text="신고 상세를 불러오는 중입니다" />}
        {expanded && (
          <ErrorAlert
            error={detailQuery.error}
            onRetry={() => void detailQuery.refetch()}
            retrying={detailQuery.isFetching}
          />
        )}
        {expanded && detailQuery.data && (
          <AdminReviewReportDetail
            report={detailQuery.data}
            adminKey={adminKey}
            onAuthError={onAuthError}
            onOpenReview={onOpenReview}
            onDecisionComplete={onDecisionComplete}
          />
        )}
      </Card.Body>
    </Card>
  );
}
