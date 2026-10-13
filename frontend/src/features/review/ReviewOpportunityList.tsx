import { useInfiniteQuery } from "@tanstack/react-query";
import { CalendarCheck, PackageCheck } from "lucide-react";
import { Card } from "react-bootstrap";
import { Link } from "react-router";
import { queryKeys, runForCurrentCustomer, cursorPageParams, flattenCursorPages } from "@/shared/api";
import { ErrorAlert, LoadingSpinner, CursorLoadMore } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { fetchMyReviewOpportunities } from "./api";
import { reviewTargetHref } from "./reviewTarget";
import { reviewSectionHref } from "./reviewSection";

export function ReviewOpportunityList() {
  const query = useInfiniteQuery({
    queryKey: queryKeys.member.reviews.opportunities,
    queryFn: ({ pageParam, signal }) => runForCurrentCustomer(
      () => fetchMyReviewOpportunities(pageParam, signal),
    ),
    ...cursorPageParams,
  });
  const opportunities = flattenCursorPages(query.data);

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
        error={query.isFetchNextPageError ? null : query.error}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
    ) : null;
  }

  return (
    <>
      <ErrorAlert
        error={query.isFetchNextPageError ? null : query.error}
        onRetry={() => void query.refetch()}
        retrying={query.isFetching}
      />
      <section className="review-opportunity-section" aria-labelledby="review-opportunity-heading">
        <div className="review-opportunity-heading">
          <h3 id="review-opportunity-heading" className="h5 mb-0">후기를 쓸 수 있는 내역</h3>
          <strong>불러온 {opportunities.length.toLocaleString("ko-KR")}건</strong>
        </div>
        <div className="review-opportunity-list">
          {opportunities.map((opportunity) => {
            const targetHref = reviewTargetHref(opportunity.targetType, opportunity.targetId);
            // 주문·예약 상세의 후기 칸으로 바로 연다.
            const sourceHref = opportunity.sourceType === "ORDER_ITEM" && opportunity.orderId
              ? reviewSectionHref(`/my/orders/${opportunity.orderId}`)
              : opportunity.bookingId
                ? reviewSectionHref(`/my/bookings/${opportunity.bookingId}`)
                : targetHref;
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
                      {opportunity.targetType === "PRODUCT" ? "작품 수령" : "클래스 이용"} 완료 · {formatDateTime(opportunity.completedAt)}
                    </small>
                  </div>
                  <Link to={sourceHref} className="btn btn-sm btn-dark">
                    후기 작성
                  </Link>
                </Card.Body>
              </Card>
            );
          })}
        </div>
        <CursorLoadMore
          query={query}
          label="작성 가능한 이용 내역 더 보기"
          variant="outline-dark"
          wrapperClassName="text-center mt-3"
        />
      </section>
    </>
  );
}
