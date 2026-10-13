import { LinkButton } from "@/shared/ui/LinkButton";
import { Badge } from "react-bootstrap";
import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchMyInquiriesPage } from "@/features/my-inquiry/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState, CursorLoadMore } from "@/shared/ui";
import { MySectionPage } from "@/features/my/MySectionPage";
import { formatDateTime } from "@/shared/lib";
import { queryKeys, cursorPageParams, flattenCursorPages } from "@/shared/api";

export function MyInquiriesPage() {
  const { isAuthenticated } = useCustomerAuth();

  const inquiriesQuery = useInfiniteQuery({
    queryKey: queryKeys.member.inquiryHistory,
    queryFn: ({ pageParam, signal }) => fetchMyInquiriesPage(pageParam, signal),
    ...cursorPageParams,
    enabled: isAuthenticated,
  });
  const inquiries = flattenCursorPages(inquiriesQuery.data);
  const hasLoadedInquiries = inquiriesQuery.data !== undefined;

  return (
    <MySectionPage
      path="/my/inquiries"
      actions={(
        <LinkButton to="/my/inquiries/new" variant="primary" size="sm">
          문의 작성
        </LinkButton>
      )}
    >
      {inquiriesQuery.isLoading && <LoadingSpinner />}
      <ErrorAlert
        error={inquiriesQuery.isFetchNextPageError ? null : inquiriesQuery.error}
        onRetry={() => { void inquiriesQuery.refetch(); }}
        retrying={inquiriesQuery.isFetching && !inquiriesQuery.isFetchingNextPage}
      />

      {hasLoadedInquiries && inquiries.length === 0 && (
        <EmptyState message="등록된 문의가 없습니다." />
      )}

      {inquiries.map((inquiry) => (
        <article key={inquiry.id} className="my-inquiry-card">
          <header>
            <Badge bg={inquiry.hasReply ? "success" : "secondary"}>
              {inquiry.hasReply ? "답변 완료" : "답변 대기"}
            </Badge>
            <h2>{inquiry.title}</h2>
            <time dateTime={inquiry.createdAt}>{formatDateTime(inquiry.createdAt)}</time>
          </header>
          <p className="my-inquiry-body">{inquiry.content}</p>
          {inquiry.replyContent && (
            <div className="my-inquiry-reply">
              <strong>공방 답변</strong>
              <p>{inquiry.replyContent}</p>
            </div>
          )}
        </article>
      ))}

      <CursorLoadMore query={inquiriesQuery} label="문의 더 보기" loadingLabel="문의 불러오는 중..." />
    </MySectionPage>
  );
}
