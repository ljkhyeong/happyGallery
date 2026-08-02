import { useQuery } from "@tanstack/react-query";
import { Card } from "react-bootstrap";
import { fetchMyProductQna, fetchProductQna } from "./api";
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
  return <ProductQnaContent key={sessionVersion} productId={productId} />;
}

function ProductQnaContent({ productId }: Props) {
  const { isAuthenticated } = useCustomerAuth();

  const {
    data: qnaList,
    error: qnaError,
    isLoading,
    isFetching: qnaFetching,
    refetch: refetchQna,
  } = useQuery({
    queryKey: ["product-qna", productId],
    queryFn: () => fetchProductQna(productId),
  });
  const {
    data: myQnaList,
    error: myQnaError,
    isFetching: myQnaFetching,
    refetch: refetchMyQna,
  } = useQuery({
    queryKey: queryKeys.member.productQna.byProduct(productId),
    queryFn: ({ signal }) => fetchMyProductQna(productId, signal),
    enabled: isAuthenticated,
  });
  const ownedQnaIds = new Set(myQnaList?.map((qna) => qna.id));
  const ownershipLoaded = !isAuthenticated || myQnaList !== undefined;

  return (
    <Card className="mt-4">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="mb-0">Q&A</h5>
          {isAuthenticated && <QnaCreateForm productId={productId} />}
        </div>

        {isLoading && <LoadingSpinner />}

        <ErrorAlert
          error={qnaError}
          onRetry={() => void refetchQna()}
          retrying={qnaFetching}
        />

        {!isLoading && qnaList && qnaList.length === 0 && (
          <EmptyState message="등록된 Q&A가 없습니다." />
        )}

        {isAuthenticated && (
          <ErrorAlert
            error={myQnaError}
            onRetry={() => void refetchMyQna()}
            retrying={myQnaFetching}
          />
        )}

        {qnaList?.map((item) => (
          <QnaItem
            key={item.id}
            item={item}
            productId={productId}
            owned={ownershipLoaded ? ownedQnaIds.has(item.id) : undefined}
          />
        ))}

        {!isAuthenticated && (
          <p className="text-muted-soft small mt-2 mb-0">
            Q&A 작성은 로그인 후 이용 가능합니다.
          </p>
        )}
      </Card.Body>
    </Card>
  );
}
