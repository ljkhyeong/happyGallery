import { useId, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Form, Modal } from "react-bootstrap";
import { queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { AdminReviewEvidence } from "./AdminReviewEvidence";
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
  report,
  adminKey,
  onAuthError,
  onOpenReview,
  onDecisionComplete,
}: {
  report: AdminReviewReportResponse;
  adminKey: string;
  onAuthError: () => void;
  onOpenReview: (reviewId: number) => void;
  onDecisionComplete: () => void;
}) {
  const confirmationTitleId = useId();
  const [note, setNote] = useState("");
  const [pendingDecision, setPendingDecision] = useState<ReviewReportDecision | null>(null);
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
      setPendingDecision(null);
      setNote("");
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.reports.all });
      requestAnimationFrame(onDecisionComplete);
      toast.show(updated.status === "ACCEPTED" ? "위반 신고로 인정했습니다." : "신고를 반려했습니다.");
    },
  });
  const closeConfirmation = () => {
    if (mutation.isPending) return;
    setPendingDecision(null);
    mutation.reset();
  };

  return (
    <Card className="admin-review-card admin-review-report-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-2">
          <div className="d-flex flex-wrap align-items-center gap-2">
            <Badge bg={report.status === "PENDING" ? "warning" : report.status === "ACCEPTED" ? "danger" : "secondary"}>
              {report.status === "PENDING" ? "처리 대기" : report.status === "ACCEPTED" ? "위반 인정" : "신고 반려"}
            </Badge>
            <strong>{REPORT_REASON_LABELS[report.reason]}</strong>
          </div>
          <small className="text-muted-soft">{formatDateTime(report.createdAt)}</small>
        </div>
        <div className="small text-muted-soft mt-3 mb-2">
          후기 #{report.reviewId} · 신고 회원 #{report.reporterUserId} · 신고 당시 {report.snapshotStatus === "PUBLISHED" ? "공개" : "숨김"}
        </div>
        <AdminReviewEvidence
          evidence={report.evidence}
          adminKey={adminKey}
          onAuthError={onAuthError}
          unavailableMessage="이 신고는 이전 데이터라 당시 후기 증거를 복구할 수 없습니다."
        />
        <Button
          type="button"
          size="sm"
          variant="outline-primary"
          className="mt-2"
          onClick={() => onOpenReview(report.reviewId)}
        >
          현재 후기 확인·관리
        </Button>
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
              <Button
                type="button"
                size="sm"
                variant="danger"
                disabled={mutation.isPending}
                aria-haspopup="dialog"
                onClick={() => {
                  mutation.reset();
                  setPendingDecision("ACCEPTED");
                }}
              >
                위반 인정
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline-secondary"
                disabled={mutation.isPending}
                aria-haspopup="dialog"
                onClick={() => {
                  mutation.reset();
                  setPendingDecision("REJECTED");
                }}
              >
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
      </Card.Body>
      <Modal
        show={pendingDecision !== null}
        onHide={closeConfirmation}
        centered
        aria-labelledby={confirmationTitleId}
      >
        <Modal.Header closeButton={!mutation.isPending} closeLabel="닫기">
          <Modal.Title id={confirmationTitleId} className="fs-6">
            {pendingDecision === "ACCEPTED" ? "위반 신고로 인정할까요?" : "신고를 반려할까요?"}
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="warning" className="small">
            저장하면 이 신고를 다시 판단할 수 없습니다. 신고 #{report.id}의 내용과 처리 방향을 다시 확인해 주세요.
          </Alert>
          <p className="small mb-2">
            {pendingDecision === "ACCEPTED"
              ? "위반 신고로 인정합니다. 후기 공개 상태는 자동으로 바뀌지 않으며 별도로 관리해야 합니다."
              : "운영 정책 위반 신고를 반려하고 처리를 끝냅니다."}
          </p>
          {note.trim() && (
            <div className="small text-muted-soft">
              처리 메모: {note.trim()}
            </div>
          )}
          <div className="mt-3"><ErrorAlert error={mutation.error} /></div>
        </Modal.Body>
        <Modal.Footer>
          <Button
            type="button"
            variant="outline-secondary"
            disabled={mutation.isPending}
            onClick={closeConfirmation}
          >
            취소
          </Button>
          <Button
            type="button"
            variant={pendingDecision === "ACCEPTED" ? "danger" : "dark"}
            disabled={mutation.isPending || pendingDecision === null}
            onClick={() => {
              if (pendingDecision) mutation.mutate(pendingDecision);
            }}
          >
            {mutation.isPending
              ? "처리 중..."
              : pendingDecision === "ACCEPTED" ? "위반 인정 확정" : "신고 반려 확정"}
          </Button>
        </Modal.Footer>
      </Modal>
    </Card>
  );
}
