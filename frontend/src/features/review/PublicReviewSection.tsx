import { useMemo, useState } from "react";
import { useInfiniteQuery, useQueries } from "@tanstack/react-query";
import { Button } from "react-bootstrap";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import {
  fetchClassReviews,
  fetchMyReviewReactions,
  fetchProductReviews,
  type ReviewSort,
} from "./api";
import { PublicReviewCard } from "./PublicReviewCard";
import { ReviewFilters } from "./ReviewFilters";
import { ReviewHistogram } from "./ReviewHistogram";
import { chunkReviewIds } from "./reviewUiPolicy";

interface Props {
  targetType: "PRODUCT" | "CLASS";
  targetId: number;
}

export function PublicReviewSection(props: Props) {
  const { sessionVersion } = useCustomerAuth();
  return <PublicReviewContent key={sessionVersion} {...props} />;
}

function PublicReviewContent({ targetType, targetId }: Props) {
  const { isAuthenticated } = useCustomerAuth();
  const [rating, setRating] = useState<number | undefined>();
  const [sort, setSort] = useState<ReviewSort>("LATEST");
  const query = useInfiniteQuery({
    queryKey: targetType === "PRODUCT"
      ? queryKeys.reviews.products.history(targetId, rating, sort)
      : queryKeys.reviews.classes.history(targetId, rating, sort),
    queryFn: ({ pageParam, signal }) => targetType === "PRODUCT"
      ? fetchProductReviews(targetId, { rating, sort }, pageParam, signal)
      : fetchClassReviews(targetId, { rating, sort }, pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore
      ? lastPage.nextCursor ?? undefined
      : undefined,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });

  const reviews = useMemo(
    () => query.data?.pages.flatMap((page) => page.content) ?? [],
    [query.data?.pages],
  );
  const summary = query.data?.pages[0]?.summary;
  const filteredCount = query.data?.pages[0]?.filteredCount ?? 0;
  const reviewIds = useMemo(() => reviews.map(({ id }) => id), [reviews]);
  const reactionIdChunks = useMemo(() => chunkReviewIds(reviewIds), [reviewIds]);
  const reactionQueries = useQueries({
    queries: isAuthenticated
      ? reactionIdChunks.map((ids) => ({
          queryKey: queryKeys.member.reviews.reactions(ids),
          queryFn: ({ signal }: { signal: AbortSignal }) => runForCurrentCustomer(
            () => fetchMyReviewReactions(ids, signal),
          ),
        }))
      : [],
  });
  const reactions = new Map(
    reactionQueries.flatMap((reactionQuery) => reactionQuery.data ?? [])
      .map((reaction) => [reaction.reviewId, reaction]),
  );
  const reactionErrorQuery = reactionQueries.find(({ error }) => error);
  const reactionsLoading = isAuthenticated && reactionQueries.some(({ isLoading }) => isLoading);
  const loginHref = buildAuthPageHref("/login", {
    redirectTo: targetType === "PRODUCT" ? `/products/${targetId}` : `/classes/${targetId}`,
  });

  return (
    <section className="review-section" aria-labelledby={`${targetType}-${targetId}-reviews`}>
      <header className="review-section-header">
        <div>
          <p className="store-section-kicker mb-1">Reviews</p>
          <h3 id={`${targetType}-${targetId}-reviews`} className="mb-1">이용 후기</h3>
        </div>
        {summary && (
          <div className="review-summary" aria-label={`평균 별점 ${summary.averageRating.toFixed(1)}점, 후기 ${summary.reviewCount}개`}>
            <strong>{summary.averageRating.toFixed(1)}</strong>
            <span aria-hidden="true">★</span>
            <small>{summary.reviewCount.toLocaleString("ko-KR")}개</small>
          </div>
        )}
      </header>

      {summary && (
        <div className="review-overview">
          <ReviewHistogram
            summary={summary}
            selectedRating={rating}
            onSelectRating={setRating}
          />
          <ReviewFilters
            rating={rating}
            sort={sort}
            filteredCount={filteredCount}
            totalCount={summary.reviewCount}
            onRatingChange={setRating}
            onSortChange={setSort}
          />
        </div>
      )}

      {query.isLoading && <LoadingSpinner text="후기를 불러오는 중입니다" />}
      <ErrorAlert
        error={query.data === undefined ? query.error : null}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
      <ErrorAlert
        error={query.data !== undefined && !query.isFetchNextPageError ? query.error : null}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
      {reactionErrorQuery && (
        <ErrorAlert
          error={reactionErrorQuery.error}
          onRetry={() => reactionQueries.forEach((reactionQuery) => void reactionQuery.refetch())}
          retrying={reactionQueries.some(({ isFetching }) => isFetching)}
        />
      )}
      {query.data !== undefined && reviews.length === 0 && (
        <EmptyState
          message={rating
            ? `${rating}점 후기가 아직 없습니다. 다른 별점을 선택해 보세요.`
            : "아직 등록된 후기가 없습니다."}
        />
      )}

      <div className="review-list">
        {reviews.map((review) => (
          <PublicReviewCard
            key={review.id}
            review={review}
            reaction={reactions.get(review.id)}
            reactionLoading={reactionsLoading}
            isAuthenticated={isAuthenticated}
            loginHref={loginHref}
            targetType={targetType}
            targetId={targetId}
          />
        ))}
      </div>

      {query.isFetchNextPageError && (
        <ErrorAlert
          error={query.error}
          onRetry={() => void query.fetchNextPage()}
          retrying={query.isFetchingNextPage}
        />
      )}

      {query.hasNextPage && (
        <div className="text-center mt-3">
          <Button
            type="button"
            variant="outline-dark"
            disabled={query.isFetchingNextPage}
            onClick={() => void query.fetchNextPage()}
          >
            {query.isFetchingNextPage ? "불러오는 중..." : "후기 더 보기"}
          </Button>
        </div>
      )}
    </section>
  );
}
