import { Badge, Table } from "react-bootstrap";
import { ArrowRight } from "lucide-react";
import type { ProductResponse, ProductVariantResponse } from "@/generated/api/adminCatalog";
import { listAdminStockLevels } from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";
import { ApiError } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";
import { fetchProducts } from "./api";

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

function variantLabel(product: ProductResponse, variant: ProductVariantResponse): string {
  if (variant.selections.length === 0) {
    return "기본 조합";
  }
  return variant.selections.map((selection) => {
    const group = product.optionGroups.find((candidate) => candidate.key === selection.groupKey);
    const value = group?.values.find((candidate) => candidate.key === selection.valueKey);
    return `${group?.name ?? "옵션"}: ${value?.name ?? "값"}`;
  }).join(" / ");
}

export function OutOfStockProductSection({ adminKey, onAuthError }: Props) {
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });

  const levels = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products", "stock-levels"],
    queryFn: () => listAdminStockLevels(undefined, { headers: adminHeaders(adminKey) }),
  });
  if (query.isLoading || levels.isLoading) return <LoadingSpinner />;
  if (levels.error) return <ErrorAlert error={levels.error} />;
  if (query.error) {
    if (query.error instanceof ApiError && query.error.status === 401) return null;
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
        <tr><th>상품</th><th>유형</th><th>재고 항목</th><th>현재 / 기준</th><th></th></tr>
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
            <td>{variant ? variantLabel(product, variant) : "기본 재고"}</td>
            <td><Badge bg={quantity === 0 ? "danger" : "warning"}>{quantity === 0 ? "품절" : "재고 부족"}</Badge> {quantity} / {minimumStock ?? 0}개</td>
            <td className="text-end">
              <LinkButton
                size="sm"
                variant="outline-primary"
                to={`/admin?view=products&productId=${product.id}${variant ? `&variantId=${variant.id}` : ""}`}
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
