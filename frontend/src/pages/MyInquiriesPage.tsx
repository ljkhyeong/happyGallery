import { LinkButton } from "@/shared/ui/LinkButton";
import { Container, Card, Badge, Button } from "react-bootstrap";
import { Link } from "react-router";
import { useInfiniteQuery } from "@tanstack/react-query";
import { fetchMyInquiriesPage } from "@/features/my-inquiry/api";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { queryKeys } from "@/shared/api";

export function MyInquiriesPage() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const loginHref = buildAuthPageHref("/login", { redirectTo: "/my/inquiries" });

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

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container">
        <Card className="text-center p-4">
          <p>로그인이 필요합니다.</p>
          <Link to={loginHref}>로그인</Link>
        </Card>
      </Container>
    );
  }

  return (
    <Container className="page-container">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h4>내 문의</h4>
        <LinkButton to="/my/inquiries/new" variant="primary" size="sm">
          문의 작성
        </LinkButton>
      </div>

      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching && !isFetchingNextPage}
      />

      {hasLoadedInquiries && inquiries.length === 0 && (
        <EmptyState message="등록된 문의가 없습니다." />
      )}

      {inquiries.length > 0 && (
        <p className="text-muted-soft small">불러온 문의 {inquiries.length}건</p>
      )}

      {inquiries.map((inquiry) => (
        <Card key={inquiry.id} className="mb-2">
          <Card.Body className="py-2 px-3">
            <div className="d-flex justify-content-between align-items-start">
              <div>
                <div className="d-flex align-items-center gap-2 mb-1">
                  <Badge bg={inquiry.hasReply ? "info" : "secondary"} className="badge-sm">
                    {inquiry.hasReply ? "답변완료" : "답변대기"}
                  </Badge>
                  <span className="fw-semibold small">{inquiry.title}</span>
                </div>
                <div className="text-muted-soft" style={{ fontSize: "0.8rem" }}>
                  {formatDateTime(inquiry.createdAt)}
                </div>
              </div>
            </div>
            <div className="mt-2 small">
              <div className="bg-light p-2 rounded">{inquiry.content}</div>
              {inquiry.replyContent && (
                <div className="mt-2 p-2 rounded" style={{ background: "#f0f4ff" }}>
                  <strong className="small">답변</strong>
                  <div>{inquiry.replyContent}</div>
                </div>
              )}
            </div>
          </Card.Body>
        </Card>
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

      <div className="mt-3">
        <Link to="/my" className="text-decoration-none">&larr; 마이페이지</Link>
      </div>
    </Container>
  );
}
