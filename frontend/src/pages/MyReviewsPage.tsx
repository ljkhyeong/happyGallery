import { useInfiniteQuery } from "@tanstack/react-query";
import { Button, Container } from "react-bootstrap";
import { Link } from "react-router";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { fetchMyReviews } from "@/features/review/api";
import { MemberReviewCard } from "@/features/review/MemberReviewCard";
import { ReviewOpportunityList } from "@/features/review/ReviewOpportunityList";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function MyReviewsPage() {
  const { sessionVersion } = useCustomerAuth();
  return <MyReviewsContent key={sessionVersion} />;
}

function MyReviewsContent() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
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

  if (authLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }
  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 720 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="내가 작성한 상품·클래스 후기는 로그인 후 확인하고 수정할 수 있습니다."
        />
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 760 }}>
      <header className="my-detail-header">
        <Link to="/my" className="text-decoration-none small">&larr; 내 정보</Link>
        <div className="my-section-kicker mt-3 mb-2">My Reviews</div>
        <h2 className="mb-2">내 후기</h2>
        <p className="text-muted-soft mb-0">
          작성한 상품·클래스 후기를 확인하고 수정하거나 삭제할 수 있습니다.
        </p>
      </header>

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
    </Container>
  );
}
