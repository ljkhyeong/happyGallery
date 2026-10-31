import { useState } from "react";
import { useQueries, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card } from "react-bootstrap";
import type { ItemDto, OrderDetailResponse } from "@/generated/api/customerStore";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { useHashScroll } from "@/shared/hooks/useHashScroll";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { fetchMyOrderReviews, fetchProductReviewCreationState } from "./api";
import { MemberReviewCard } from "./MemberReviewCard";
import { ReviewForm } from "./ReviewForm";
import { useReviewFormTriggerFocus } from "./useReviewFormFocus";
import { useCreateProductReview } from "./useReviewMutations";
import { REVIEW_SECTION_ID } from "./reviewSection";

interface Props {
  orderId: number;
  status: OrderDetailResponse["status"];
  items: ItemDto[];
}

/**
 * 작품을 받지 못하고 끝난 주문(거절·취소·자동 환불·지연 거절·미수령). 서버는 배송 완료·수령 완료·완료 주문만
 * 후기를 받으므로(OrderStatus.isReviewable) 이 주문에는 후기 구역을 두지 않는다.
 */
const ENDED_WITHOUT_RECEIPT = new Set<OrderDetailResponse["status"]>([
  "REJECTED",
  "CUSTOMER_CANCELED",
  "AUTO_REFUND_TIMEOUT",
  "DELAY_REJECTED_CANCELED",
  "PICKUP_EXPIRED",
  "PICKUP_FORFEITED",
]);

export function OrderReviewsSection({ status, ...props }: Props) {
  if (ENDED_WITHOUT_RECEIPT.has(status)) return null;
  return <OrderReviewsContent {...props} />;
}

function OrderReviewsContent({ orderId, items }: Omit<Props, "status">) {
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
  const creationStatePending = creationStateQueries.some(({ isPending }) => isPending);
  // 아직 받지 않은 주문은 작품마다 같은 안내를 되풀이하지 않고 한 줄로 알린다.
  const waitingForReceipt = reviewsQuery.data?.length === 0
    && items.length > 0
    && creationStateQueries.every(({ data }) => data?.status === "NOT_REVIEWABLE");
  useHashScroll(REVIEW_SECTION_ID);

  return (
    <section id={REVIEW_SECTION_ID} className="my-detail-page-section" aria-labelledby="order-reviews-heading">
      <header className="review-section-header">
        <div>
          <h2 id="order-reviews-heading" className="my-detail-page-title">작품 후기</h2>
          <p className="text-muted-soft small mb-0">
            {waitingForReceipt
              ? "배송·매장 수령을 마치면 받은 작품마다 후기를 한 번 쓸 수 있습니다."
              : "받은 작품마다 후기를 한 번 쓸 수 있습니다."}
          </p>
        </div>
      </header>
      {(reviewsQuery.isLoading || (reviewStateAvailable && creationStatePending)) && (
        <LoadingSpinner text="후기 작성 상태를 확인하는 중입니다" />
      )}
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

      {reviewStateAvailable && !creationStatePending && !waitingForReceipt && (
        <div className="review-list">
          {items.map((item, index) => {
            const review = reviewsQuery.data?.find(
              (candidate) => candidate.sourceType === "ORDER_ITEM" && candidate.sourceId === item.orderItemId,
            );
            const creationStatus = creationStateQueries[index]?.data?.status;
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
                  {creationStatus === "RECREATION_BLOCKED" && (
                    <Alert variant="warning" className="small mt-3 mb-0">
                      이 작품의 후기가 공방에서 비공개 처리된 적이 있어 새 후기를 작성할 수 없습니다.
                    </Alert>
                  )}
                  {creationStatus === "REVIEW_EXISTS" && (
                    <p className="text-muted-soft small mt-3 mb-0">이 작품에는 이미 후기를 작성했습니다.</p>
                  )}
                  {creationStatus === "NOT_REVIEWABLE" && (
                    <p className="text-muted-soft small mt-3 mb-0">배송·매장 수령 또는 주문이 완료된 뒤 작성할 수 있습니다.</p>
                  )}
                  {writing && creationStatus === "AVAILABLE" && (
                    <div className="mt-3 pt-3 border-top">
                      <ReviewForm
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
      )}
    </section>
  );
}
