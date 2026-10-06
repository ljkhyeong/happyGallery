import { useInfiniteQuery } from "@tanstack/react-query";
import { Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { myNavLabel } from "@/features/my/myNavigation";
import { fetchMyReviews } from "@/features/review/api";
import { MemberReviewCard } from "@/features/review/MemberReviewCard";
import { ReviewOpportunityList } from "@/features/review/ReviewOpportunityList";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { EmptyState, ErrorAlert, LoadingSpinner, PageHeader } from "@/shared/ui";

export function MyReviewsPage() {
  const { sessionVersion } = useCustomerAuth();
  return <MyReviewsContent key={sessionVersion} />;
}

function MyReviewsContent() {
  const { isAuthenticated } = useCustomerAuth();
  const reviewsQuery = useInfiniteQuery({
    queryKey: queryKeys.member.reviews.history,
    queryFn: ({ pageParam, signal }) => runForCurrentCustomer(
      () => fetchMyReviews(pageParam, signal),
    ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore
      ? lastPage.nextCursor ?? undefined
      : undefined,
    enabled: isAuthenticated,
  });
  const reviews = reviewsQuery.data?.pages.flatMap((page) => page.content) ?? [];

  return (
    <>
      <PageHeader
        title={myNavLabel("/my/reviews")}
        description="작성한 상품·클래스 후기를 확인하고 수정하거나 삭제할 수 있습니다."
      />

      <ReviewOpportunityList />

      {reviewsQuery.isLoading && <LoadingSpinner text="내 후기를 불러오는 중입니다" />}
      <ErrorAlert
        error={reviewsQuery.data === undefined ? reviewsQuery.error : null}
        onRetry={() => void reviewsQuery.refetch()}
        retrying={reviewsQuery.isFetching}
      />
      <ErrorAlert
        error={reviewsQuery.data !== undefined && !reviewsQuery.isFetchNextPageError
          ? reviewsQuery.error
          : null}
        onRetry={() => void reviewsQuery.refetch()}
        retrying={reviewsQuery.isFetching}
      />
      {reviewsQuery.data !== undefined && reviews.length === 0 && (
        <EmptyState message="아직 작성한 후기가 없습니다. 작성 가능한 이용 내역이나 완료된 주문·예약 상세에서 첫 후기를 남겨보세요." />
      )}
      <div className="review-list">
        {reviews.map((review) => <MemberReviewCard key={review.id} review={review} />)}
      </div>
      {reviewsQuery.isFetchNextPageError && (
        <ErrorAlert
          error={reviewsQuery.error}
          onRetry={() => void reviewsQuery.fetchNextPage()}
          retrying={reviewsQuery.isFetchingNextPage}
        />
      )}
      {reviewsQuery.hasNextPage && (
        <div className="text-center mt-3">
          <Button
            type="button"
            variant="outline-dark"
            disabled={reviewsQuery.isFetchingNextPage}
            onClick={() => void reviewsQuery.fetchNextPage()}
          >
            {reviewsQuery.isFetchingNextPage ? "불러오는 중..." : "후기 더 보기"}
          </Button>
        </div>
      )}
    </>
  );
}
