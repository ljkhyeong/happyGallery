import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card } from "react-bootstrap";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { fetchClassReviewCreationState, fetchMyBookingReviews } from "./api";
import { MemberReviewCard } from "./MemberReviewCard";
import { ReviewForm } from "./ReviewForm";
import { useReviewFormTriggerFocus } from "./useReviewFormFocus";
import { useCreateClassReview } from "./useReviewMutations";

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

  return (
    <section className="review-section member-source-reviews" aria-labelledby="booking-review-heading">
      <header className="review-section-header">
        <div>
          <p className="my-section-kicker mb-1">Review</p>
          <h5 id="booking-review-heading" className="mb-1">클래스 후기</h5>
          <p className="text-muted-soft small mb-0">
            {creationStateQuery.data?.status === "AVAILABLE"
              ? `${className} 수업 경험을 들려주세요.`
              : "공방에서 수업 완료로 처리한 뒤 후기를 작성할 수 있습니다."}
          </p>
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
        <p className="text-muted-soft small mb-0">이 클래스 이용 건에는 이미 후기가 등록되어 있습니다.</p>
      )}
      {!review && creationStateQuery.data?.status === "NOT_REVIEWABLE" && (
        <p className="text-muted-soft small mb-0">공방에서 수업 완료로 처리한 뒤 후기를 작성할 수 있습니다.</p>
      )}
      {reviewStateAvailable && writing && !review && creationStateQuery.data?.status === "AVAILABLE" && (
        <Card className="review-card">
          <Card.Body>
            <ReviewForm
              autoFocusFirstInput
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
