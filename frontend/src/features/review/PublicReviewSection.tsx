import { useMemo, useState } from "react";
import { useInfiniteQuery, useQueries } from "@tanstack/react-query";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys, runForCurrentCustomer, cursorPageParams, flattenCursorPages } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { EmptyState, ErrorAlert, LoadingSpinner, CursorLoadMore } from "@/shared/ui";
import {
  fetchMyReviewReactions,
  fetchTargetReviews,
  type ReviewSort,
} from "./api";
import { PublicReviewCard } from "./PublicReviewCard";
import { ReviewFilters } from "./ReviewFilters";
import { ReviewHistogram } from "./ReviewHistogram";
import { chunkReviewIdsByPage } from "./reviewUiPolicy";
import { reviewTargetHref } from "./reviewTarget";

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
    queryKey: queryKeys.reviews.historyByTarget(targetType, targetId, rating, sort),
    queryFn: ({ pageParam, signal }) =>
      fetchTargetReviews(targetType, targetId, { rating, sort }, pageParam, signal),
    ...cursorPageParams,
    staleTime: PUBLIC_DATA_STALE_TIME,
  });

  const reviews = useMemo(() => flattenCursorPages(query.data), [query.data]);
  // 후기가 하나도 없으면 0점 분포·필터 대신 빈 상태 안내만 보여 준다.
  const firstPageSummary = query.data?.pages[0]?.summary;
  const summary = firstPageSummary && firstPageSummary.reviewCount > 0 ? firstPageSummary : undefined;
  const filteredCount = query.data?.pages[0]?.filteredCount ?? 0;
  const reactionIdChunks = useMemo(
    () => chunkReviewIdsByPage(
      query.data?.pages.map((page) => page.content.map(({ id }) => id)) ?? [],
    ),
    [query.data?.pages],
  );
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
  const reactionLoadingByReviewId = new Map<number, boolean>();
  reactionIdChunks.forEach((reviewIds, index) => {
    const isLoading = reactionQueries[index]?.isLoading ?? false;
    reviewIds.forEach((reviewId) => reactionLoadingByReviewId.set(reviewId, isLoading));
  });
  const reactionErrorQuery = reactionQueries.find(({ error }) => error);
  // 로그인 뒤 후기 칸으로 돌아온다. 작품 상세는 위에 붙는 탭 아래에 멈추는 #product-reviews를 쓴다.
  const reviewsAnchor = targetType === "PRODUCT" ? "product-reviews" : `${targetType}-${targetId}-reviews`;
  const loginHref = buildAuthPageHref("/login", {
    redirectTo: `${reviewTargetHref(targetType, targetId)}#${reviewsAnchor}`,
  });

  return (
    <section className="review-section" aria-labelledby={`${targetType}-${targetId}-reviews`}>
      <header className="review-section-header">
        <h2 id={`${targetType}-${targetId}-reviews`}>후기</h2>
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
        error={query.isFetchNextPageError ? null : query.error}
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
            ? `${rating}점 후기가 없습니다.`
            : "아직 등록된 후기가 없습니다."}
        />
      )}

      <div className="review-list">
        {reviews.map((review) => (
          <PublicReviewCard
            key={review.id}
            review={review}
            reaction={reactions.get(review.id)}
            reactionLoading={reactionLoadingByReviewId.get(review.id) ?? false}
            isAuthenticated={isAuthenticated}
            loginHref={loginHref}
            targetType={targetType}
            targetId={targetId}
          />
        ))}
      </div>

      <CursorLoadMore
        query={query}
        label="후기 더 보기"
        variant="outline-dark"
        wrapperClassName="text-center mt-3"
      />
    </section>
  );
}
