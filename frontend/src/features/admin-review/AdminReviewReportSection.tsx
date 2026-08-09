import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Form } from "react-bootstrap";
import { queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { ReviewStars } from "@/features/review/ReviewDisplay";
import {
  decideReviewReport,
  fetchAdminReviewReports,
  type AdminReviewReportResponse,
  type ReviewReportDecision,
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
}: {
  adminKey: string;
  onAuthError: () => void;
}) {
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
          <h6 id="admin-review-report-heading" className="mb-1">후기 신고</h6>
          <p className="text-muted-soft small mb-0">신고 판단과 후기 숨김은 별도 운영 절차입니다.</p>
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
  report,
  adminKey,
  onAuthError,
}: {
  report: AdminReviewReportResponse;
  adminKey: string;
  onAuthError: () => void;
}) {
  const [note, setNote] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: (decision: ReviewReportDecision) => decideReviewReport(
      adminKey,
      report.id,
      decision,
      note,
    ),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.reports.all });
      setNote("");
      toast.show(updated.status === "ACCEPTED" ? "위반 신고로 인정했습니다." : "신고를 반려했습니다.");
    },
  });

  return (
    <Card className="admin-review-card admin-review-report-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <Badge bg={report.status === "PENDING" ? "warning" : report.status === "ACCEPTED" ? "danger" : "secondary"}>
              {report.status === "PENDING" ? "처리 대기" : report.status === "ACCEPTED" ? "위반 인정" : "신고 반려"}
            </Badge>
            <strong>{REPORT_REASON_LABELS[report.reason]}</strong>
            <ReviewStars rating={report.snapshotRating} />
          </div>
          <small className="text-muted-soft">{formatDateTime(report.createdAt)}</small>
        </div>
        <p className="admin-review-content mt-3 mb-2">{report.snapshotContent}</p>
        <div className="small text-muted-soft">
          후기 #{report.reviewId} · 신고 회원 #{report.reporterUserId} · 신고 당시 {report.snapshotStatus === "PUBLISHED" ? "공개" : "숨김"}
          {report.snapshotEditedAt && ` · 마지막 수정 ${formatDateTime(report.snapshotEditedAt)}`}
        </div>
        {report.detail && <Alert variant="light" className="small mt-3 mb-0">신고 상세: {report.detail}</Alert>}
        {report.status === "PENDING" ? (
          <Form.Group controlId={`admin-review-report-note-${report.id}`} className="mt-3">
            <Form.Label>처리 메모 <span className="text-muted-soft">(선택)</span></Form.Label>
            <Form.Control
              as="textarea"
              rows={2}
              maxLength={1000}
              value={note}
              disabled={mutation.isPending}
              onChange={(event) => setNote(event.target.value)}
            />
            <div className="d-flex flex-wrap gap-2 mt-2">
              <Button type="button" size="sm" variant="danger" disabled={mutation.isPending} onClick={() => mutation.mutate("ACCEPTED")}>
                위반 인정
              </Button>
              <Button type="button" size="sm" variant="outline-secondary" disabled={mutation.isPending} onClick={() => mutation.mutate("REJECTED")}>
                신고 반려
              </Button>
            </div>
          </Form.Group>
        ) : (
          <div className="small mt-3">
            처리 관리자 #{report.decidedByAdminId} · {report.decidedAt ? formatDateTime(report.decidedAt) : "처리 시각 없음"}
            {report.decisionNote && <p className="mb-0 mt-1">메모: {report.decisionNote}</p>}
          </div>
        )}
        <div className="mt-2"><ErrorAlert error={mutation.error} /></div>
      </Card.Body>
    </Card>
  );
}
