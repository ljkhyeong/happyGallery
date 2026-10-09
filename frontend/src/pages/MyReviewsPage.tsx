import { useInfiniteQuery } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MySectionPage } from "@/features/my/MySectionPage";
import { fetchMyReviews } from "@/features/review/api";
import { MemberReviewCard } from "@/features/review/MemberReviewCard";
import { ReviewOpportunityList } from "@/features/review/ReviewOpportunityList";
import { queryKeys, runForCurrentCustomer, cursorPageParams, flattenCursorPages } from "@/shared/api";
import { EmptyState, ErrorAlert, LoadingSpinner, CursorLoadMore } from "@/shared/ui";

export function MyReviewsPage() {
  const { isAuthenticated } = useCustomerAuth();
  const reviewsQuery = useInfiniteQuery({
    queryKey: queryKeys.member.reviews.history,
    queryFn: ({ pageParam, signal }) => runForCurrentCustomer(
      () => fetchMyReviews(pageParam, signal),
    ),
    ...cursorPageParams,
    enabled: isAuthenticated,
  });
  const reviews = flattenCursorPages(reviewsQuery.data);

  return (
    <MySectionPage
      path="/my/reviews"
      description="작성한 상품·클래스 후기를 확인하고 수정하거나 삭제할 수 있습니다."
    >
      <ReviewOpportunityList />

      {reviewsQuery.isLoading && <LoadingSpinner text="내 후기를 불러오는 중입니다" />}
      <ErrorAlert
        error={reviewsQuery.isFetchNextPageError ? null : reviewsQuery.error}
        onRetry={() => void reviewsQuery.refetch()}
        retrying={reviewsQuery.isFetching}
      />
      {reviewsQuery.data !== undefined && reviews.length === 0 && (
        <EmptyState message="아직 작성한 후기가 없습니다. 작성 가능한 이용 내역이나 완료된 주문·예약 상세에서 첫 후기를 남겨보세요." />
      )}
      <div className="review-list">
        {reviews.map((review) => <MemberReviewCard key={review.id} review={review} />)}
      </div>
      <CursorLoadMore
        query={reviewsQuery}
        label="후기 더 보기"
        variant="outline-dark"
        wrapperClassName="text-center mt-3"
      />
    </MySectionPage>
  );
}
