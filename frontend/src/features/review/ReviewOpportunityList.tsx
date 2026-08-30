import { useInfiniteQuery } from "@tanstack/react-query";
import { ArrowRight, CalendarCheck, PackageCheck } from "lucide-react";
import { Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { fetchMyReviewOpportunities } from "./api";

export function ReviewOpportunityList() {
  const query = useInfiniteQuery({
    queryKey: queryKeys.member.reviews.opportunities,
    queryFn: ({ pageParam, signal }) => runForCurrentCustomer(
      () => fetchMyReviewOpportunities(pageParam, signal),
    ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.hasMore
      ? lastPage.nextCursor ?? undefined
      : undefined,
  });
  const opportunities = query.data?.pages.flatMap((page) => page.content) ?? [];

  if (query.isLoading) {
    return <LoadingSpinner text="작성할 수 있는 후기를 확인하는 중입니다" />;
  }

  if (query.data === undefined && query.error) {
    return (
      <ErrorAlert
        error={query.error}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
    );
  }

  if (query.data !== undefined && opportunities.length === 0) {
    return query.error ? (
      <ErrorAlert
        error={query.data !== undefined && !query.isFetchNextPageError ? query.error : null}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
    ) : null;
  }

  return (
    <>
      <ErrorAlert
        error={query.error}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
      <section className="review-opportunity-section" aria-labelledby="review-opportunity-heading">
        <div className="review-opportunity-heading">
          <div>
            <p className="my-section-kicker mb-1">Ready to review</p>
            <h3 id="review-opportunity-heading" className="h5 mb-1">후기를 기다리는 이용 내역</h3>
            <p className="text-muted-soft small mb-0">완료된 이용 경험을 다른 고객과 나눠주세요.</p>
          </div>
          <strong>불러온 {opportunities.length.toLocaleString("ko-KR")}건</strong>
        </div>
        <div className="review-opportunity-list">
          {opportunities.map((opportunity) => {
            const sourceHref = opportunity.sourceType === "ORDER_ITEM" && opportunity.orderId
              ? `/my/orders/${opportunity.orderId}`
              : opportunity.bookingId
                ? `/my/bookings/${opportunity.bookingId}`
                : opportunity.targetType === "PRODUCT"
                  ? `/products/${opportunity.targetId}`
                  : `/classes/${opportunity.targetId}`;
            const targetHref = opportunity.targetType === "PRODUCT"
              ? `/products/${opportunity.targetId}`
              : `/classes/${opportunity.targetId}`;
            const Icon = opportunity.targetType === "PRODUCT" ? PackageCheck : CalendarCheck;

            return (
              <Card key={`${opportunity.sourceType}-${opportunity.sourceId}`} className="review-opportunity-card">
                <Card.Body>
                  <div className="review-opportunity-icon" aria-hidden="true"><Icon size={20} /></div>
                  <div className="review-opportunity-copy">
                    <Link to={targetHref} className="fw-semibold text-decoration-none">
                      {opportunity.targetName}
                    </Link>
                    <small className="text-muted-soft">
                      {opportunity.targetType === "PRODUCT" ? "상품 수령" : "클래스 이용"} 완료 · {formatDateTime(opportunity.completedAt)}
                    </small>
                  </div>
                  <Link to={sourceHref} className="btn btn-sm btn-dark">
                    후기 작성 <ArrowRight size={14} aria-hidden="true" />
                  </Link>
                </Card.Body>
              </Card>
            );
          })}
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
              {query.isFetchingNextPage ? "불러오는 중..." : "작성 가능한 이용 내역 더 보기"}
            </Button>
          </div>
        )}
      </section>
    </>
  );
}
