import { useState } from "react";
import { ChevronDown, ChevronUp } from "lucide-react";
import { Button } from "react-bootstrap";
import { queryKeys } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { AdminReviewEvidence } from "./AdminReviewEvidence";
import { fetchReviewModerationActions } from "./api";

interface Props {
  reviewId: number;
  adminKey: string;
  onAuthError: () => void;
}

const REVIEW_VISIBILITY_LABEL: Record<string, string> = {
  PUBLISHED: "공개",
  HIDDEN: "비공개",
};

export function AdminReviewModerationTimeline({ reviewId, adminKey, onAuthError }: Props) {
  const [expanded, setExpanded] = useState(false);
  const query = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.reviews.moderation(reviewId),
    queryFn: ({ signal }) => fetchReviewModerationActions(adminKey, reviewId, signal),
    enabled: expanded,
  });

  return (
    <section className="admin-review-audit mt-3" aria-label="후기 운영 이력">
      <Button
        type="button"
        size="sm"
        variant="link"
        className="px-0"
        aria-expanded={expanded}
        onClick={() => setExpanded((current) => !current)}
      >
        {expanded ? <ChevronUp size={15} aria-hidden="true" /> : <ChevronDown size={15} aria-hidden="true" />}
        운영 이력 {expanded ? "접기" : "보기"}
      </Button>
      {expanded && (
        <div className="admin-review-audit-content">
          {query.isLoading && <LoadingSpinner text="운영 이력을 불러오는 중입니다" />}
          <ErrorAlert
            error={query.error}
            onRetry={() => void query.refetch()}
            retrying={query.isFetching}
          />
          {query.data?.length === 0 && <EmptyState message="아직 비공개·재공개 이력이 없습니다." />}
          {query.data && query.data.length > 0 && (
            <ol className="admin-review-timeline">
              {query.data.map((action) => (
                <li key={action.id}>
                  <strong>{action.action === "HIDE" ? "후기 비공개" : "후기 재공개"}</strong>
                  <span>
                    {REVIEW_VISIBILITY_LABEL[action.previousStatus] ?? "상태 확인 필요"}
                    {" → "}
                    {REVIEW_VISIBILITY_LABEL[action.newStatus] ?? "상태 확인 필요"}
                  </span>
                  {action.reason && <p>{action.reason}</p>}
                  <small>관리자 #{action.adminUserId} · {formatDateTime(action.createdAt)}</small>
                  <AdminReviewEvidence
                    evidence={action.evidence}
                    adminKey={adminKey}
                    onAuthError={onAuthError}
                    unavailableMessage="이전 운영 이력이라 당시 후기 증거를 복구할 수 없습니다."
                  />
                </li>
              ))}
            </ol>
          )}
        </div>
      )}
    </section>
  );
}
