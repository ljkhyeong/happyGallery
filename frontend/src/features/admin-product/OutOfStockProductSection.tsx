import { Badge, Table } from "react-bootstrap";
import { ArrowRight } from "lucide-react";
import type { ProductResponse, ProductVariantResponse } from "@/generated/api/adminCatalog";
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

function outOfStockItems(products: ProductResponse[]): OutOfStockItem[] {
  return products.flatMap((product) => {
    if (product.status !== "ACTIVE") {
      return [];
    }
    if (product.type === "READY_STOCK") {
      return product.quantity === 0 ? [{ product }] : [];
    }
    return product.variants
      .filter((variant) => variant.active && variant.quantity === 0)
      .map((variant) => ({ product, variant }));
  });
}

export function OutOfStockProductSection({ adminKey, onAuthError }: Props) {
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });

  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) {
    if (query.error instanceof ApiError && query.error.status === 401) return null;
    return <ErrorAlert error={query.error} />;
  }

  const items = outOfStockItems(query.data ?? []);
  if (items.length === 0) {
    return <EmptyState message="재고를 채워야 할 판매 중 상품이 없습니다." />;
  }

  return (
    <Table responsive hover size="sm" className="mb-0">
      <thead>
        <tr><th>상품</th><th>유형</th><th>품절 항목</th><th></th></tr>
      </thead>
      <tbody>
        {items.map(({ product, variant }) => (
          <tr key={`${product.id}:${variant?.id ?? "inventory"}`}>
            <td>{product.name}</td>
            <td>
              <Badge bg={product.type === "READY_STOCK" ? "secondary" : "info"}>
                {product.type === "READY_STOCK" ? "기성품" : "주문제작"}
              </Badge>
            </td>
            <td>{variant ? variantLabel(product, variant) : "기본 재고"}</td>
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
