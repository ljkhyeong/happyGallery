import { useState } from "react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card } from "react-bootstrap";
import type { ItemDto } from "@/generated/api/customerStore";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { fetchMyOrderReviews, fetchProductReviewCreationState } from "./api";
import { MemberReviewCard } from "./MemberReviewCard";
import { ReviewForm } from "./ReviewForm";
import { useReviewFormTriggerFocus } from "./useReviewFormFocus";
import { useCreateProductReview } from "./useReviewMutations";

interface Props {
  orderId: number;
  items: ItemDto[];
}

export function OrderReviewsSection({ orderId, items }: Props) {
  const [activeOrderItemId, setActiveOrderItemId] = useState<number | null>(null);
  const reviewsQuery = useQuery({
    queryKey: queryKeys.member.reviews.byOrder(orderId),
    queryFn: ({ signal }) => runForCurrentCustomer(() => fetchMyOrderReviews(orderId, signal)),
  });
  const creationStateQueries = useQueries({
    queries: items.map((item) => ({
      queryKey: queryKeys.member.reviews.productCreationState(item.orderItemId),
      queryFn: ({ signal }: { signal: AbortSignal }) => runForCurrentCustomer(
        () => fetchProductReviewCreationState(item.orderItemId, signal),
      ),
    })),
  });
  const createMutation = useCreateProductReview(() => setActiveOrderItemId(null));
  const rememberWritingTrigger = useReviewFormTriggerFocus(activeOrderItemId !== null);
  const reviewStateAvailable = reviewsQuery.data !== undefined;
  const creationStateError = creationStateQueries.find(({ error }) => error);

  return (
    <section className="review-section member-source-reviews" aria-labelledby="order-reviews-heading">
      <header className="review-section-header">
        <div>
          <p className="my-section-kicker mb-1">Review</p>
          <h5 id="order-reviews-heading" className="mb-1">상품 후기</h5>
          <p className="text-muted-soft small mb-0">
            서버가 확인한 완료 상품마다 후기를 한 번씩 남길 수 있습니다.
          </p>
        </div>
      </header>
      {reviewsQuery.isLoading && <LoadingSpinner text="후기 작성 상태를 확인하는 중입니다" />}
      <ErrorAlert
        error={reviewsQuery.error}
        onRetry={() => void reviewsQuery.refetch()}
        retrying={reviewsQuery.isFetching}
      />
      {creationStateError && (
        <ErrorAlert
          error={creationStateError.error}
          onRetry={() => creationStateQueries.forEach((query) => void query.refetch())}
          retrying={creationStateQueries.some(({ isFetching }) => isFetching)}
        />
      )}

      <div className="review-list">
        {reviewStateAvailable && items.map((item, index) => {
          const review = reviewsQuery.data?.find(
            (candidate) => candidate.sourceType === "ORDER_ITEM" && candidate.sourceId === item.orderItemId,
          );
          const creationStateQuery = creationStateQueries[index];
          const creationStatus = creationStateQuery?.data?.status;
          const writing = activeOrderItemId === item.orderItemId;
          return review ? (
            <MemberReviewCard key={review.id} review={review} />
          ) : (
            <Card key={item.orderItemId} className="review-card">
              <Card.Body>
                <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
                  <div>
                    <strong>{item.productName}</strong>
                    <div className="text-muted-soft small mt-1">수량 {item.qty}개</div>
                  </div>
                  {creationStatus === "AVAILABLE" && !writing && (
                    <Button
                      id={`order-item-review-write-${item.orderItemId}`}
                      type="button"
                      size="sm"
                      variant="outline-dark"
                      onClick={(event) => {
                        rememberWritingTrigger(event.currentTarget);
                        createMutation.reset();
                        setActiveOrderItemId(item.orderItemId);
                      }}
                    >
                      후기 작성
                    </Button>
                  )}
                </div>
                {creationStateQuery?.isLoading && (
                  <div className="mt-3"><LoadingSpinner text="작성 가능 여부를 확인하는 중입니다" /></div>
                )}
                {creationStatus === "RECREATION_BLOCKED" && (
                  <Alert variant="warning" className="small mt-3 mb-0">
                    운영 정책으로 숨김 처리된 이력이 있어 이 상품 이용 건에는 후기를 다시 작성할 수 없습니다.
                  </Alert>
                )}
                {creationStatus === "REVIEW_EXISTS" && (
                  <p className="text-muted-soft small mt-3 mb-0">이 상품 이용 건에는 이미 후기가 등록되어 있습니다.</p>
                )}
                {creationStatus === "NOT_REVIEWABLE" && (
                  <p className="text-muted-soft small mt-3 mb-0">배송·픽업 또는 주문 완료 후 작성할 수 있습니다.</p>
                )}
                {writing && creationStatus === "AVAILABLE" && (
                  <div className="mt-3 pt-3 border-top">
                    <ReviewForm
                      autoFocusFirstInput
                      pending={createMutation.isPending}
                      error={createMutation.error}
                      onCancel={() => {
                        createMutation.reset();
                        setActiveOrderItemId(null);
                      }}
                      onSubmit={(value) => createMutation.mutate({ orderItemId: item.orderItemId, ...value })}
                    />
                  </div>
                )}
              </Card.Body>
            </Card>
          );
        })}
      </div>
    </section>
  );
}
