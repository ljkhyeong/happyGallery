import type { QueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/shared/api";
import type { ProductType } from "@/shared/types";
import type { OptionGroupDraft, VariantDraft } from "./ProductOptionEditor";

const PRODUCTION_LEAD_DAYS_MAX = 180;
const PRODUCT_VARIANTS_MAX = 500;

export interface ProductFormValues {
  type: ProductType;
  name: string;
  price: string;
  quantity: string;
  /** 등록은 기존 재고 상품에 1개 이상, 수정과 주문제작 기본 조합은 0개 이상을 요구한다. */
  minimumQuantity: number;
  specification: string;
  productionLeadDays: string;
  optionGroups: OptionGroupDraft[];
  variants: VariantDraft[];
}

/** 주문제작 상품은 온라인 주문에 적용할 사양과 제작 기간(1~180일)이 있어야 한다. */
function purchaseTermsValid({ type, specification, productionLeadDays }: ProductFormValues): boolean {
  if (type !== "MADE_TO_ORDER") return true;
  const leadDays = Number(productionLeadDays);
  return specification.trim().length > 0
    && Number.isInteger(leadDays)
    && leadDays >= 1
    && leadDays <= PRODUCTION_LEAD_DAYS_MAX;
}

/** 옵션 이름·선택 값 이름이 비어 있지 않고, 조합 재고와 조합별 최종 가격이 맞아야 한다. */
function optionsValid({ type, price, optionGroups, variants }: ProductFormValues): boolean {
  if (type !== "MADE_TO_ORDER") return true;
  return optionGroups.every((group) => group.name.trim().length > 0
      && (group.type !== "SELECT"
        || group.values.every((value) => value.name.trim().length > 0)))
    && variants.length <= PRODUCT_VARIANTS_MAX
    && variants.every((variant) => variant.quantity >= 0
      && Number(price) + variant.priceAdjustment > 0);
}

export function isProductFormValid(values: ProductFormValues): boolean {
  return values.name.trim().length > 0
    && Number(values.price) > 0
    && Number(values.quantity) >= values.minimumQuantity
    && purchaseTermsValid(values)
    && optionsValid(values);
}

/** 상품 등록·수정·판매 상태 변경 뒤 관리자 목록과 공개 상품 목록·카테고리를 다시 읽는다. */
export function invalidateProductQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: queryKeys.admin.products.all });
  void queryClient.invalidateQueries({ queryKey: queryKeys.catalog.products });
  void queryClient.invalidateQueries({ queryKey: queryKeys.catalog.productCategories });
}
