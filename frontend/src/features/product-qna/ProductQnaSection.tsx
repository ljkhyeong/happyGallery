import { useInfiniteQuery } from "@tanstack/react-query";
import { Button, Card } from "react-bootstrap";
import { fetchMyProductQnaPage, fetchProductQnaPage } from "./api";
import { QnaItem } from "./QnaItem";
import { QnaCreateForm } from "./QnaCreateForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys } from "@/shared/api";
import { LoadingSpinner, EmptyState, ErrorAlert } from "@/shared/ui";

interface Props {
  productId: number;
}

export function ProductQnaSection({ productId }: Props) {
  const { sessionVersion } = useCustomerAuth();
  return <ProductQnaContent key={`${sessionVersion}:${productId}`} productId={productId} />;
}

function ProductQnaContent({ productId }: Props) {
  const { isAuthenticated } = useCustomerAuth();

  const {
    data: qnaData,
    error: qnaError,
    isLoading,
    isFetching: qnaFetching,
    isFetchingNextPage: qnaFetchingNextPage,
    hasNextPage: qnaHasNextPage,
    fetchNextPage: fetchNextQnaPage,
    refetch: refetchQna,
  } = useInfiniteQuery({
    queryKey: queryKeys.productQna.history(productId),
    queryFn: ({ pageParam, signal }) =>
      fetchProductQnaPage(productId, pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
  });
  const {
    data: myQnaData,
    error: myQnaError,
    isFetching: myQnaFetching,
    isFetchingNextPage: myQnaFetchingNextPage,
    hasNextPage: myQnaHasNextPage,
    fetchNextPage: fetchNextMyQnaPage,
    refetch: refetchMyQna,
  } = useInfiniteQuery({
    queryKey: queryKeys.member.productQna.history(productId),
    queryFn: ({ pageParam, signal }) =>
      fetchMyProductQnaPage(productId, pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: isAuthenticated,
  });
  const qnaPages = qnaData?.pages ?? [];
  const myQnaPages = myQnaData?.pages ?? [];
  const qnaList = qnaPages.flatMap((page) => page.content);
  const myQnaList = myQnaPages.flatMap((page) => page.content);
  const ownedQnaIds = new Set(myQnaList.map((qna) => qna.id));
  const ownershipBehind = isAuthenticated
    && myQnaData !== undefined
    && myQnaHasNextPage === true
    && myQnaPages.length < qnaPages.length;
  const ownershipLoaded = !isAuthenticated || (
    myQnaData !== undefined
    && (
      myQnaHasNextPage === false
      || myQnaPages.length >= qnaPages.length
    )
  );
  const canLoadMore = qnaHasNextPage === true || ownershipBehind;
  const loadingMore = qnaFetchingNextPage || myQnaFetchingNextPage;

  const loadNextPage = async () => {
    if (ownershipBehind) {
      await fetchNextMyQnaPage();
      return;
    }

    const nextPublicPageCount = qnaPages.length + 1;
    await Promise.all([
      qnaHasNextPage ? fetchNextQnaPage() : Promise.resolve(),
      isAuthenticated
        && myQnaHasNextPage
        && myQnaPages.length < nextPublicPageCount
        ? fetchNextMyQnaPage()
        : Promise.resolve(),
    ]);
  };

  return (
    <Card className="mt-4">
      <Card.Body>
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
          <h5 className="mb-0">상품 문의</h5>
          {isAuthenticated && <QnaCreateForm productId={productId} />}
        </div>

        {isLoading && <LoadingSpinner />}

        <ErrorAlert
          error={qnaError}
          onRetry={() => void refetchQna()}
          retrying={qnaFetching}
        />

        {!isLoading && qnaData && qnaList.length === 0 && (
          <EmptyState message="등록된 상품 문의가 없습니다." />
        )}

        {isAuthenticated && (
          <ErrorAlert
            error={myQnaError}
            onRetry={() => void refetchMyQna()}
            retrying={myQnaFetching}
          />
        )}

        {qnaList.length > 0 && (
          <p className="text-muted-soft small">조회된 문의 {qnaList.length}건</p>
        )}

        {qnaList.map((item) => (
          <QnaItem
            key={item.id}
            item={item}
            productId={productId}
            owned={ownershipLoaded ? ownedQnaIds.has(item.id) : undefined}
          />
        ))}

        {canLoadMore && (
          <div className="d-grid mt-3">
            <Button
              type="button"
              variant="outline-primary"
              disabled={
                loadingMore
                || (isAuthenticated && myQnaData === undefined)
              }
              onClick={() => { void loadNextPage(); }}
            >
              {loadingMore
                ? "문의 불러오는 중..."
                : qnaHasNextPage
                  ? "문의 더 보기"
                  : "내 문의 더 보기"}
            </Button>
          </div>
        )}

        {!isAuthenticated && (
          <p className="text-muted-soft small mt-2 mb-0">
            로그인 후 상품 문의를 작성할 수 있습니다.
          </p>
        )}
      </Card.Body>
    </Card>
  );
}
