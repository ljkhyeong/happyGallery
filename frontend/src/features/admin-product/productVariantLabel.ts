import type { ProductResponse, ProductVariantResponse } from "@/generated/api/adminCatalog";

/** 관리자 화면의 옵션 조합 이름. 선택 옵션이 없으면 기본 조합이다. */
export function productVariantLabel(product: ProductResponse, variant: ProductVariantResponse): string {
  if (variant.selections.length === 0) return "기본 조합";
  return variant.selections.map((selection) => {
    const group = product.optionGroups.find((candidate) => candidate.key === selection.groupKey);
    const value = group?.values.find((candidate) => candidate.key === selection.valueKey);
    return `${group?.name ?? "옵션"}: ${value?.name ?? "값"}`;
  }).join(" / ");
}
