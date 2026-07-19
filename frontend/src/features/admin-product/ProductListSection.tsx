import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Table } from "react-bootstrap";
import { fetchProducts, updateProductStatus } from "./api";
import { InventoryAdjustmentModal } from "./InventoryAdjustmentModal";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatKRW, PRODUCT_TYPE_LABEL } from "@/shared/lib";
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function ProductListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [pendingStatusId, setPendingStatusId] = useState<number | null>(null);
  const { data: products, isLoading, error } = useQuery({
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onAuthError();
    }
  }, [error, onAuthError]);

  const statusMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      updateProductStatus(adminKey, id, active ? "ACTIVE" : "INACTIVE"),
    onMutate: ({ id }) => setPendingStatusId(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin", "products"] }),
    onSettled: () => setPendingStatusId(null),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!products?.length) return <EmptyState message="등록된 상품이 없습니다." />;

  const selectedProduct = products?.find((product) => product.id === selectedProductId) ?? null;

  return (
    <>
      <ErrorAlert error={statusMutation.error} />
      <Table responsive hover size="sm">
        <thead>
          <tr>
            <th>ID</th>
            <th>상품명</th>
            <th>유형</th>
            <th className="text-end">가격</th>
            <th className="text-end">수량</th>
            <th>판매 상태</th>
            <th>재고 상태</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id}>
              <td>{product.id}</td>
              <td>{product.name}</td>
              <td>{PRODUCT_TYPE_LABEL[product.type] ?? product.type}</td>
              <td className="text-end">{formatKRW(product.price)}</td>
              <td className="text-end">{product.quantity}</td>
              <td>
                <Badge bg={product.status === "ACTIVE" ? "success" : "secondary"}>
                  {product.status === "ACTIVE" ? "판매 중" : "판매 중지"}
                </Badge>
              </td>
              <td>
                <Badge bg={product.quantity > 0 ? "primary" : "secondary"}>
                  {product.quantity > 0 ? "재고 있음" : "품절"}
                </Badge>
              </td>
              <td>
                <div className="d-flex gap-2 justify-content-end" style={{ minWidth: 190 }}>
                  <Button
                    size="sm"
                    variant="outline-primary"
                    onClick={() => setSelectedProductId(product.id)}
                  >
                    재고 조정
                  </Button>
                  <Button
                    size="sm"
                    variant={product.status === "ACTIVE" ? "outline-danger" : "outline-success"}
                    disabled={pendingStatusId === product.id}
                    onClick={() => statusMutation.mutate({
                      id: product.id,
                      active: product.status !== "ACTIVE",
                    })}
                  >
                    {pendingStatusId === product.id
                      ? "처리 중..."
                      : product.status === "ACTIVE" ? "판매 중지" : "판매 재개"}
                  </Button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>

      <InventoryAdjustmentModal
        adminKey={adminKey}
        product={selectedProduct}
        onClose={() => setSelectedProductId(null)}
        onAuthError={onAuthError}
      />
    </>
  );
}
