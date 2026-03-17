import { useQuery } from "@tanstack/react-query";
import { Card } from "react-bootstrap";
import { fetchProductQna } from "./api";
import { QnaItem } from "./QnaItem";
import { QnaCreateForm } from "./QnaCreateForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, EmptyState } from "@/shared/ui";

interface Props {
  productId: number;
}

export function ProductQnaSection({ productId }: Props) {
  const { isAuthenticated } = useCustomerAuth();

  const { data: qnaList, isLoading } = useQuery({
    queryKey: ["product-qna", productId],
    queryFn: () => fetchProductQna(productId),
  });

  return (
    <Card className="mt-4">
      <Card.Body>
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="mb-0">Q&A</h5>
          {isAuthenticated && <QnaCreateForm productId={productId} />}
        </div>

        {isLoading && <LoadingSpinner />}

        {!isLoading && (!qnaList || qnaList.length === 0) && (
          <EmptyState message="등록된 Q&A가 없습니다." />
        )}

        {qnaList?.map((item) => (
          <QnaItem key={item.id} item={item} productId={productId} />
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
