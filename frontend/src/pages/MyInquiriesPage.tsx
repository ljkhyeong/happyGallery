import { LinkButton } from "@/shared/ui/LinkButton";
import { Badge, Button } from "react-bootstrap";
import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchMyInquiriesPage } from "@/features/my-inquiry/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState, PageHeader } from "@/shared/ui";
import { myNavLabel } from "@/features/my/myNavigation";
import { formatDateTime } from "@/shared/lib";
import { queryKeys } from "@/shared/api";

export function MyInquiriesPage() {
  const { isAuthenticated } = useCustomerAuth();

  const {
    data: inquiriesData,
    isLoading,
    isFetching,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: queryKeys.member.inquiryHistory,
    queryFn: ({ pageParam, signal }) => fetchMyInquiriesPage(pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: isAuthenticated,
  });
  const inquiries = inquiriesData?.pages.flatMap((page) => page.content) ?? [];
  const hasLoadedInquiries = inquiriesData !== undefined;

  return (
    <>
      <PageHeader
        title={myNavLabel("/my/inquiries")}
        description="상품·예약·이용 관련 문의를 남기고 공방의 답변을 확인하세요."
        actions={(
          <LinkButton to="/my/inquiries/new" variant="primary" size="sm">
            문의 작성
          </LinkButton>
        )}
      />

      {isLoading && <LoadingSpinner />}
      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching && !isFetchingNextPage}
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

      {hasNextPage && (
        <div className="d-grid mt-3">
          <Button
            type="button"
            variant="outline-primary"
            disabled={isFetchingNextPage}
            onClick={() => { void fetchNextPage(); }}
          >
            {isFetchingNextPage ? "문의 불러오는 중..." : "문의 더 보기"}
          </Button>
        </div>
      )}
    </>
  );
}
