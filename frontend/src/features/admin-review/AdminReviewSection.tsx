import { useRef, useState, type KeyboardEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
import { queryKeys } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { ReviewStars, ReviewStatusBadge } from "@/features/review/ReviewDisplay";
import { ReviewImageGallery } from "@/features/review/ReviewImageGallery";
import { ReviewTrustBadges } from "@/features/review/ReviewTrustBadges";
import { AdminReviewModerationTimeline } from "./AdminReviewModerationTimeline";
import { AdminReviewReplyForm } from "./AdminReviewReplyForm";
import { AdminReviewReportSection } from "./AdminReviewReportSection";
import {
  changeAdminReviewStatus,
  fetchAdminReviews,
  type AdminReviewResponse,
  type ListAdminReviewsStatus,
  type ListAdminReviewsTargetType,
} from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function AdminReviewSection({ adminKey, onAuthError }: Props) {
  const [activePane, setActivePane] = useState<"reviews" | "reports">("reviews");
  const reviewTabRef = useRef<HTMLButtonElement>(null);
  const reportTabRef = useRef<HTMLButtonElement>(null);
  const [targetType, setTargetType] = useState<ListAdminReviewsTargetType | "">("");
  const [status, setStatus] = useState<ListAdminReviewsStatus | "">("");
  const { cursor, hasPreviousPage, showNextPage, showPreviousPage, resetCursor } = useCursorHistory();
  const reviewsQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.reviews.page(targetType || undefined, status || undefined, cursor),
    queryFn: ({ signal }) => fetchAdminReviews(adminKey, {
      targetType: targetType || undefined,
      status: status || undefined,
      cursor,
    }, signal),
    enabled: activePane === "reviews",
  });
  const handleTabKeyDown = (
    event: KeyboardEvent<HTMLButtonElement>,
    currentPane: "reviews" | "reports",
  ) => {
    let nextPane: "reviews" | "reports" | null = null;
    if (event.key === "Home") nextPane = "reviews";
    if (event.key === "End") nextPane = "reports";
    if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
      nextPane = currentPane === "reviews" ? "reports" : "reviews";
    }
    if (!nextPane) return;

    event.preventDefault();
    setActivePane(nextPane);
    (nextPane === "reviews" ? reviewTabRef : reportTabRef).current?.focus();
  };

  return (
    <div>
      <div className="admin-review-tabs" role="tablist" aria-label="후기 운영 메뉴">
        <Button
          type="button"
          size="sm"
          variant={activePane === "reviews" ? "dark" : "outline-secondary"}
          ref={reviewTabRef}
          id="admin-review-tab"
          role="tab"
          aria-controls="admin-review-panel"
          aria-selected={activePane === "reviews"}
          tabIndex={activePane === "reviews" ? 0 : -1}
          onClick={() => setActivePane("reviews")}
          onKeyDown={(event) => handleTabKeyDown(event, "reviews")}
        >
          후기 관리
        </Button>
        <Button
          type="button"
          size="sm"
          variant={activePane === "reports" ? "dark" : "outline-secondary"}
          ref={reportTabRef}
          id="admin-review-report-tab"
          role="tab"
          aria-controls="admin-review-report-panel"
          aria-selected={activePane === "reports"}
          tabIndex={activePane === "reports" ? 0 : -1}
          onClick={() => setActivePane("reports")}
          onKeyDown={(event) => handleTabKeyDown(event, "reports")}
        >
          신고 관리
        </Button>
      </div>

      {activePane === "reports" ? (
        <div id="admin-review-report-panel" role="tabpanel" aria-labelledby="admin-review-report-tab">
          <AdminReviewReportSection adminKey={adminKey} onAuthError={onAuthError} />
        </div>
      ) : (
        <div id="admin-review-panel" role="tabpanel" aria-labelledby="admin-review-tab">
          <Row className="g-2 mb-3">
            <Col sm={6} md={4}>
              <Form.Label htmlFor="admin-review-target">후기 종류</Form.Label>
              <Form.Select
                id="admin-review-target"
                value={targetType}
                onChange={(event) => {
                  setTargetType(event.target.value as ListAdminReviewsTargetType | "");
                  resetCursor();
                }}
              >
                <option value="">전체</option>
                <option value="PRODUCT">상품</option>
                <option value="CLASS">클래스</option>
              </Form.Select>
            </Col>
            <Col sm={6} md={4}>
              <Form.Label htmlFor="admin-review-status">공개 상태</Form.Label>
              <Form.Select
                id="admin-review-status"
                value={status}
                onChange={(event) => {
                  setStatus(event.target.value as ListAdminReviewsStatus | "");
                  resetCursor();
                }}
              >
                <option value="">전체</option>
                <option value="PUBLISHED">공개</option>
                <option value="HIDDEN">숨김</option>
              </Form.Select>
            </Col>
          </Row>

          {reviewsQuery.isLoading && <LoadingSpinner text="후기를 불러오는 중입니다" />}
          <ErrorAlert
            error={reviewsQuery.error}
            onRetry={() => void reviewsQuery.refetch()}
            retrying={reviewsQuery.isFetching}
          />
          {reviewsQuery.data?.content.length === 0 && <EmptyState message="조건에 맞는 후기가 없습니다." />}
          <div className="admin-review-list">
            {reviewsQuery.data?.content.map((review) => (
              <AdminReviewCard
                key={review.id}
                review={review}
                adminKey={adminKey}
                onAuthError={onAuthError}
              />
            ))}
          </div>
          {(hasPreviousPage || reviewsQuery.data?.hasMore) && (
            <div className="d-flex justify-content-center gap-2 mt-3">
              <Button
                type="button"
                size="sm"
                variant="outline-secondary"
                disabled={!hasPreviousPage || reviewsQuery.isFetching}
                onClick={showPreviousPage}
              >
                이전
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline-primary"
                disabled={!reviewsQuery.data?.hasMore || reviewsQuery.isFetching}
                onClick={() => showNextPage(reviewsQuery.data?.nextCursor)}
              >
                다음
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function AdminReviewCard({
  review,
  adminKey,
  onAuthError,
}: {
  review: AdminReviewResponse;
  adminKey: string;
  onAuthError: () => void;
}) {
  const [showHideForm, setShowHideForm] = useState(false);
  const [reason, setReason] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: (input: { status: "PUBLISHED" | "HIDDEN"; reason?: string }) =>
      changeAdminReviewStatus(adminKey, review.id, input.status, input.reason),
    onSuccess: async (updated) => {
      const publicKey = updated.targetType === "PRODUCT"
        ? queryKeys.reviews.products.byProduct(updated.targetId)
        : queryKeys.reviews.classes.byClass(updated.targetId);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
        queryClient.invalidateQueries({ queryKey: publicKey }),
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.moderation(updated.id) }),
      ]);
      setReason("");
      setShowHideForm(false);
      toast.show(updated.status === "HIDDEN" ? "후기를 숨겼습니다." : "후기를 다시 공개했습니다.");
    },
  });

  return (
    <Card className="admin-review-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
              <ReviewStatusBadge status={review.status} />
              <Badge bg="light" text="dark">{review.targetType === "PRODUCT" ? "상품" : "클래스"}</Badge>
              <ReviewStars rating={review.rating} />
              <ReviewTrustBadges
                verifiedTransaction={review.verifiedTransaction}
                edited={review.edited}
              />
            </div>
            <strong>{review.targetName}</strong>
            <div className="small text-muted-soft mt-1">
              {review.authorName} (회원 #{review.userId}) · {formatDateTime(review.createdAt)}
            </div>
          </div>
          {review.status === "PUBLISHED" ? (
            <Button
              type="button"
              size="sm"
              variant="outline-danger"
              onClick={() => {
                mutation.reset();
                setShowHideForm(true);
              }}
            >
              숨기기
            </Button>
          ) : (
            <Button
              type="button"
              size="sm"
              variant="outline-primary"
              disabled={mutation.isPending}
              onClick={() => mutation.mutate({ status: "PUBLISHED" })}
            >
              다시 공개
            </Button>
          )}
        </div>
        <p className="admin-review-content mt-3 mb-0">{review.content}</p>
        <ReviewImageGallery images={review.images} label="후기 첨부 사진" />
        {review.status === "HIDDEN" && (
          <Alert variant="warning" className="small mt-3 mb-0">
            숨김 사유: {review.hiddenReason || "사유 없음"}
          </Alert>
        )}
        {showHideForm && review.status === "PUBLISHED" && (
          <Form
            className="mt-3 pt-3 border-top"
            onSubmit={(event) => {
              event.preventDefault();
              if (reason.trim()) mutation.mutate({ status: "HIDDEN", reason });
            }}
          >
            <Form.Group controlId={`admin-review-reason-${review.id}`}>
              <Form.Label>숨김 사유</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                maxLength={500}
                required
                value={reason}
                disabled={mutation.isPending}
                onChange={(event) => setReason(event.target.value)}
              />
            </Form.Group>
            <div className="d-flex gap-2 mt-2">
              <Button type="submit" size="sm" variant="danger" disabled={mutation.isPending || !reason.trim()}>
                {mutation.isPending ? "처리 중..." : "숨김 확정"}
              </Button>
              <Button
                type="button"
                size="sm"
                variant="outline-secondary"
                disabled={mutation.isPending}
                onClick={() => {
                  mutation.reset();
                  setShowHideForm(false);
                }}
              >
                취소
              </Button>
            </div>
          </Form>
        )}
        <div className="mt-2"><ErrorAlert error={mutation.error} /></div>
        <AdminReviewReplyForm review={review} adminKey={adminKey} onAuthError={onAuthError} />
        <AdminReviewModerationTimeline reviewId={review.id} adminKey={adminKey} onAuthError={onAuthError} />
      </Card.Body>
    </Card>
  );
}
