import { useEffect, useRef, useState, type KeyboardEvent } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Badge, Button, Card, Col, Form, Row } from "react-bootstrap";
import { queryKeys } from "@/shared/api";
import { formatDateTime } from "@/shared/lib";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useCursorHistory } from "@/shared/hooks/useCursorHistory";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import { ReviewStars, ReviewStatusBadge } from "@/features/review/ReviewDisplay";
import { ReviewImageGallery } from "@/features/review/ReviewImageGallery";
import { ReviewTrustBadges } from "@/features/review/ReviewTrustBadges";
import { isAdminReviewMutationConflict } from "@/features/review/reviewMutationConflict";
import { AdminReviewModerationTimeline } from "./AdminReviewModerationTimeline";
import { AdminReviewProtectedImage } from "./AdminReviewProtectedImage";
import { AdminReviewReplyForm } from "./AdminReviewReplyForm";
import { AdminReviewReportSection } from "./AdminReviewReportSection";
import {
  changeAdminReviewStatus,
  fetchAdminReview,
  fetchAdminReviews,
  type AdminReviewResponse,
  type ListAdminReviewsStatus,
  type ListAdminReviewsTargetType,
} from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  focusedReviewId?: number;
  onFocusedReviewChange: (reviewId?: number) => void;
}

export function AdminReviewSection({
  adminKey,
  onAuthError,
  focusedReviewId,
  onFocusedReviewChange,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [activePane, setActivePane] = useState<"reviews" | "reports">("reviews");
  const reviewTabRef = useRef<HTMLButtonElement>(null);
  const reportTabRef = useRef<HTMLButtonElement>(null);
  const focusedReviewHeadingRef = useRef<HTMLHeadingElement>(null);
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
    enabled: activePane === "reviews" && focusedReviewId === undefined,
  });
  const focusedReviewQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.reviews.detail(focusedReviewId ?? 0),
    queryFn: ({ signal }) => fetchAdminReview(adminKey, focusedReviewId!, signal),
    enabled: activePane === "reviews" && focusedReviewId !== undefined,
  });
  const openReview = (reviewId: number) => {
    setActivePane("reviews");
    onFocusedReviewChange(reviewId);
  };
  const selectPane = (pane: "reviews" | "reports") => {
    if (focusedReviewId !== undefined) onFocusedReviewChange(undefined);
    setActivePane(pane);
  };
  const handleRevisionConflict = async (reviewId: number) => {
    const detailKey = queryKeys.admin.reviews.detail(reviewId);
    let refreshed = false;
    try {
      await queryClient.cancelQueries({ queryKey: detailKey });
      const latest = await fetchAdminReview(adminKey, reviewId);
      queryClient.setQueryData(detailKey, latest);
      refreshed = true;
    } catch (error) {
      if (isAdminSessionUnauthorized(error)) {
        onAuthError();
        return;
      }
      queryClient.removeQueries({ queryKey: detailKey, exact: true });
    }
    openReview(reviewId);
    requestAnimationFrame(() => focusedReviewHeadingRef.current?.focus());
    toast.show(
      refreshed
        ? "후기가 다른 작업으로 변경되어 최신 상태를 다시 불러왔습니다. 내용을 확인한 뒤 다시 처리해 주세요."
        : "후기가 변경되었지만 최신 내용을 불러오지 못했습니다. 상세 화면에서 다시 시도해 주세요.",
      "warning",
    );
  };

  useEffect(() => {
    if (focusedReviewId !== undefined) setActivePane("reviews");
  }, [focusedReviewId]);

  useEffect(() => {
    if (activePane !== "reviews" || focusedReviewId === undefined) return;
    const frame = requestAnimationFrame(() => focusedReviewHeadingRef.current?.focus());
    return () => cancelAnimationFrame(frame);
  }, [activePane, focusedReviewId]);
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
    selectPane(nextPane);
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
          onClick={() => selectPane("reviews")}
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
          onClick={() => selectPane("reports")}
          onKeyDown={(event) => handleTabKeyDown(event, "reports")}
        >
          신고 관리
        </Button>
      </div>

      {activePane === "reports" ? (
        <div id="admin-review-report-panel" role="tabpanel" aria-labelledby="admin-review-report-tab">
          <AdminReviewReportSection
            adminKey={adminKey}
            onAuthError={onAuthError}
            onOpenReview={openReview}
          />
        </div>
      ) : (
        <div id="admin-review-panel" role="tabpanel" aria-labelledby="admin-review-tab">
          {focusedReviewId !== undefined ? (
            <section aria-labelledby="admin-focused-review-heading">
              <div className="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3">
                <h6
                  ref={focusedReviewHeadingRef}
                  id="admin-focused-review-heading"
                  className="mb-0"
                  tabIndex={-1}
                >
                  신고 대상 후기 #{focusedReviewId}
                </h6>
                <Button
                  type="button"
                  size="sm"
                  variant="outline-secondary"
                  onClick={() => onFocusedReviewChange(undefined)}
                >
                  전체 후기 목록
                </Button>
              </div>
              {focusedReviewQuery.isLoading && <LoadingSpinner text="최신 후기를 불러오는 중입니다" />}
              <ErrorAlert
                error={focusedReviewQuery.error}
                onRetry={() => void focusedReviewQuery.refetch()}
                retrying={focusedReviewQuery.isFetching}
              />
              {focusedReviewQuery.data && (
                <AdminReviewCard
                  review={focusedReviewQuery.data}
                  adminKey={adminKey}
                  onAuthError={onAuthError}
                  onRevisionConflict={(reviewId) => void handleRevisionConflict(reviewId)}
                />
              )}
            </section>
          ) : (
            <>
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
                <option value="HIDDEN">비공개</option>
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
                onRevisionConflict={(reviewId) => void handleRevisionConflict(reviewId)}
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
            </>
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
  onRevisionConflict,
}: {
  review: AdminReviewResponse;
  adminKey: string;
  onAuthError: () => void;
  onRevisionConflict: (reviewId: number) => void;
}) {
  const [showHideForm, setShowHideForm] = useState(false);
  const [reason, setReason] = useState("");
  const queryClient = useQueryClient();
  const toast = useToast();
  const synchronizeReview = async (updated: AdminReviewResponse) => {
    queryClient.setQueryData(queryKeys.admin.reviews.detail(updated.id), updated);
    const publicKey = updated.targetType === "PRODUCT"
      ? queryKeys.reviews.products.byProduct(updated.targetId)
      : queryKeys.reviews.classes.byClass(updated.targetId);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.reviews.all }),
      queryClient.invalidateQueries({ queryKey: queryKeys.member.reviews.all }),
      queryClient.invalidateQueries({ queryKey: publicKey }),
    ]);
  };
  const mutation = useAdminMutation(onAuthError, {
    mutationFn: (input: { status: "PUBLISHED" | "HIDDEN"; reason?: string }) =>
      changeAdminReviewStatus(
        adminKey,
        review.id,
        input.status,
        review.contentRevision,
        review.version,
        input.reason,
      ),
    onSuccess: async (updated) => {
      await synchronizeReview(updated);
      setReason("");
      setShowHideForm(false);
      toast.show(updated.status === "HIDDEN" ? "후기를 숨겼습니다." : "후기를 다시 공개했습니다.");
    },
    onError: (error) => {
      if (isAdminReviewMutationConflict(error)) {
        onRevisionConflict(review.id);
      }
    },
  });
  const revisionConflict = isAdminReviewMutationConflict(mutation.error);

  return (
    <Card className="admin-review-card">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3">
          <div>
            <div className="d-flex flex-wrap align-items-center gap-2 mb-2">
              <ReviewStatusBadge status={review.status} />
              <Badge bg="light" text="dark">{review.targetType === "PRODUCT" ? "상품" : "클래스"}</Badge>
              <Badge bg="light" text="dark">본문 버전 {review.contentRevision}</Badge>
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
        {review.status === "HIDDEN" ? (
          review.images.length > 0 && (
            <div className="review-image-gallery" aria-label="비공개 후기 첨부 사진">
              {review.images.map((image, index) => (
                <AdminReviewProtectedImage
                  key={image.id}
                  adminKey={adminKey}
                  source={{ kind: "review", reviewId: review.id, imageId: image.id }}
                  alt={`비공개 후기 첨부 사진 ${index + 1}`}
                  onAuthError={onAuthError}
                />
              ))}
            </div>
          )
        ) : (
          <ReviewImageGallery images={review.images} label="후기 첨부 사진" />
        )}
        {review.status === "HIDDEN" && (
          <Alert variant="warning" className="small mt-3 mb-0">
            비공개 사유: {review.hiddenReason || "사유 없음"}
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
              <Form.Label>비공개 사유</Form.Label>
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
                {mutation.isPending ? "처리 중..." : "비공개 확정"}
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
        <div className="mt-2">
          {revisionConflict ? (
            <Alert variant="warning" className="small mb-3">
              후기가 다른 작업으로 변경되었습니다. 최신 상태를 다시 불러왔으니 내용을 확인한 뒤 다시 처리해 주세요.
            </Alert>
          ) : (
            <ErrorAlert error={mutation.error} />
          )}
        </div>
        <AdminReviewReplyForm
          review={review}
          adminKey={adminKey}
          onAuthError={onAuthError}
          onUpdated={(updated) => synchronizeReview(updated)}
          onRevisionConflict={onRevisionConflict}
        />
        <AdminReviewModerationTimeline reviewId={review.id} adminKey={adminKey} onAuthError={onAuthError} />
      </Card.Body>
    </Card>
  );
}
