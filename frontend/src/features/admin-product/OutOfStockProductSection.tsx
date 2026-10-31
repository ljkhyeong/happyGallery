import { Badge, Table } from "react-bootstrap";
import { ArrowRight } from "lucide-react";
import type { ProductResponse, ProductVariantResponse } from "@/generated/api/adminCatalog";
import { listAdminStockLevels } from "@/generated/api/adminCatalog";
import { adminHeaders, queryKeys } from "@/shared/api";
import { isAdminSessionUnauthorized } from "@/shared/hooks/adminSessionUnauthorized";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { adminProductHref } from "@/shared/lib/adminLinks";
import { EmptyState, ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";
import { adminProductsQuery } from "./api";
import { productVariantLabel } from "./productVariantLabel";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

interface OutOfStockItem {
  product: ProductResponse;
  variant?: ProductVariantResponse;
  quantity: number;
  minimumStock: number | null;
}

export function OutOfStockProductSection({ adminKey, onAuthError }: Props) {
  const query = useAdminQuery(onAuthError, adminProductsQuery(adminKey));

  const levels = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.products.stockLevels,
    queryFn: () => listAdminStockLevels(undefined, { headers: adminHeaders(adminKey) }),
  });
  if (query.isLoading || levels.isLoading) return <LoadingSpinner />;
  if (levels.error) return <ErrorAlert error={levels.error} />;
  if (query.error) {
    if (isAdminSessionUnauthorized(query.error)) return null;
    return <ErrorAlert error={query.error} />;
  }

  const items: OutOfStockItem[] = (levels.data ?? []).filter((level) => level.lowStock).flatMap((level) => {
    const product = query.data?.find((value) => value.id === level.productId);
    if (!product) return [];
    const variant = product.variants.find((value) => value.id === level.productVariantId);
    return [{ product, variant, quantity: level.quantity, minimumStock: level.minimumStock }];
  });
  if (items.length === 0) {
    return <EmptyState message="재고를 채워야 할 판매 중 상품이 없습니다." />;
  }

  return (
    <Table responsive hover size="sm" className="mb-0">
      <thead>
        <tr><th>상품</th><th>유형</th><th>재고 항목</th><th>현재 재고 / 최소 보유 수량</th><th></th></tr>
      </thead>
      <tbody>
        {items.map(({ product, variant, quantity, minimumStock }) => (
          <tr key={`${product.id}:${variant?.id ?? "inventory"}`}>
            <td>{product.name}</td>
            <td>
              <Badge bg={product.type === "READY_STOCK" ? "secondary" : "info"}>
                {product.type === "READY_STOCK" ? "기성품" : "주문제작"}
              </Badge>
            </td>
            <td>{variant ? productVariantLabel(product, variant) : "기본 재고"}</td>
            <td><Badge bg={quantity === 0 ? "danger" : "warning"}>{quantity === 0 ? "품절" : "재고 부족"}</Badge> {quantity} / {minimumStock ?? 0}개</td>
            <td className="text-end">
              <LinkButton
                size="sm"
                variant="outline-primary"
                to={adminProductHref(product.id, variant?.id)}
              >
                재고 조정 <ArrowRight size={14} aria-hidden="true" />
              </LinkButton>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
