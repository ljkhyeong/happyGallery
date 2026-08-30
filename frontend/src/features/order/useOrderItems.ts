import { useQuery } from "@tanstack/react-query";
import { fetchProducts } from "@/features/product/api";
import { productSelectionView } from "@/features/product/productSelectionView";
import { productQuantities, productQuantityLimit, productSkuKey } from "@/features/product/purchaseStock";
import { productOptionLineKey } from "@/features/product/optionLineKey";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import type { OrderItemInput } from "@/shared/types";

export function useOrderItems(requestedItems: OrderItemInput[]) {
  const query = useQuery({
    queryKey: ["products"],
    queryFn: () => fetchProducts(),
    staleTime: PUBLIC_DATA_STALE_TIME,
  });
  const productMap = new Map(query.data?.map((product) => [product.id, product]));
  const selections = requestedItems.map((item) => {
    const product = productMap.get(item.productId);
    const selection = product ? productSelectionView(product, item) : null;
    return {
      product, selection,
      item: { ...item, productVariantId: selection?.productVariantId ?? item.productVariantId ?? null },
    };
  });
  const items = selections.map(({ item }) => item);
  const quantities = productQuantities(items);
  const lines = selections.map(({ product, selection, item }) => {
    const limit = product ? productQuantityLimit(product, item.productVariantId) : 0;
    const quantity = quantities.get(productSkuKey(item)) ?? item.qty;
    const problem = !product || !product.available
      ? "현재 구매할 수 없는 상품입니다. 이 항목을 삭제해 주세요."
      : !selection?.configurationValid
        ? "선택한 옵션이 변경되었습니다. 상품 상세에서 다시 선택해 주세요."
        : quantity > limit
          ? `같은 상품·옵션 조합은 합계 ${limit}개까지 주문할 수 있습니다. 수량을 줄여 주세요.`
          : null;
    return {
      item, product, selection, problem,
      key: productOptionLineKey(item.productId, item.productVariantId, item.textInputs),
      maxQuantity: Math.max(0, limit - quantity + item.qty),
    };
  });
  return {
    query, productMap, items, lines, quantities,
    productTypes: [...new Set(selections.flatMap(({ product }) => product ? [product.type] : []))],
    itemAmount: lines.reduce((total, { item, selection, problem }) => (
      total + (problem ? 0 : (selection?.unitPrice ?? 0) * item.qty)
    ), 0),
    canPurchase: !query.isPending && !query.isError && lines.length > 0 && lines.every((line) => !line.problem),
  };
}
