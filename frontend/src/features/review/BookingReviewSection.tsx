import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card } from "react-bootstrap";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { useHashScroll } from "@/shared/hooks/useHashScroll";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { fetchClassReviewCreationState, fetchMyBookingReviews } from "./api";
import { MemberReviewCard } from "./MemberReviewCard";
import { ReviewForm } from "./ReviewForm";
import { useReviewFormTriggerFocus } from "./useReviewFormFocus";
import { useCreateClassReview } from "./useReviewMutations";
import { REVIEW_SECTION_ID } from "./reviewSection";

interface Props {
  bookingId: number;
  className: string;
}

export function BookingReviewSection({ bookingId, className }: Props) {
  const [writing, setWriting] = useState(false);
  const reviewsQuery = useQuery({
    queryKey: queryKeys.member.reviews.byBooking(bookingId),
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchMyBookingReviews(bookingId, signal)),
  });
  const creationStateQuery = useQuery({
    queryKey: queryKeys.member.reviews.classCreationState(bookingId),
    queryFn: ({ signal }) => runForCurrentCustomer(
      () => fetchClassReviewCreationState(bookingId, signal),
    ),
  });
  const createMutation = useCreateClassReview(() => setWriting(false));
  const rememberWritingTrigger = useReviewFormTriggerFocus(writing);
  const review = reviewsQuery.data?.[0];
  const reviewStateAvailable = reviewsQuery.data !== undefined;
  useHashScroll(REVIEW_SECTION_ID);

  return (
    <section id={REVIEW_SECTION_ID} className="my-detail-page-section" aria-labelledby="booking-review-heading">
      <header className="review-section-header">
        <div>
          <h2 id="booking-review-heading" className="my-detail-page-title">클래스 후기</h2>
          {/* 상태별 안내는 아래 본문이 보여 주므로, 머리에는 작성 가능·대기 상태만 적어 문장이 겹치거나 어긋나지 않게 한다. */}
          {creationStateQuery.data?.status === "AVAILABLE" && (
            <p className="text-muted-soft small mb-0">{className} 수업 후기를 쓸 수 있습니다.</p>
          )}
          {creationStateQuery.data?.status === "NOT_REVIEWABLE" && (
            <p className="text-muted-soft small mb-0">공방에서 수업 완료로 처리한 뒤 후기를 작성할 수 있습니다.</p>
          )}
        </div>
        {creationStateQuery.data?.status === "AVAILABLE" && reviewStateAvailable && !review && !writing && (
          <Button
            id={`booking-review-write-${bookingId}`}
            type="button"
            size="sm"
            variant="outline-dark"
            onClick={(event) => {
              rememberWritingTrigger(event.currentTarget);
              setWriting(true);
            }}
          >
            후기 작성
          </Button>
        )}
      </header>
      {reviewsQuery.isLoading && <LoadingSpinner text="후기 작성 상태를 확인하는 중입니다" />}
      <ErrorAlert
        error={reviewsQuery.error}
        onRetry={() => void reviewsQuery.refetch()}
        retrying={reviewsQuery.isFetching}
      />
      <ErrorAlert
        error={creationStateQuery.error}
        onRetry={() => void creationStateQuery.refetch()}
        retrying={creationStateQuery.isFetching}
      />
      {review && <MemberReviewCard review={review} />}
      {!review && creationStateQuery.data?.status === "RECREATION_BLOCKED" && (
        <Alert variant="warning" className="small mb-0">
          이 클래스의 후기가 공방에서 비공개 처리된 적이 있어 새 후기를 작성할 수 없습니다.
        </Alert>
      )}
      {!review && creationStateQuery.data?.status === "REVIEW_EXISTS" && (
        <p className="text-muted-soft small mb-0">이 예약에는 이미 후기를 작성했습니다.</p>
      )}
      {reviewStateAvailable && writing && !review && creationStateQuery.data?.status === "AVAILABLE" && (
        <Card className="review-card">
          <Card.Body>
            <ReviewForm
              pending={createMutation.isPending}
              error={createMutation.error}
              onCancel={() => {
                createMutation.reset();
                setWriting(false);
              }}
              onSubmit={(value) => createMutation.mutate({ bookingId, ...value })}
            />
          </Card.Body>
        </Card>
      )}
    </section>
  );
}
