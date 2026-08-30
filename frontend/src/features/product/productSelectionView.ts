import type { ProductDetailResponse } from "@/generated/api/product";
import type { ProductOptionSnapshotResponse, ProductTextInputRequest } from "@/generated/api/customerStore";

interface ProductSelection {
  productVariantId?: number | null;
  textInputs?: readonly ProductTextInputRequest[];
}

export function selectedProductVariant(product: ProductDetailResponse, variantId?: number | null) {
  return product.variants.find((candidate) => variantId != null
    ? candidate.id === variantId
    : product.optionGroups.length === 0 && candidate.selections.length === 0 && candidate.active);
}

export function productSelectionView(product: ProductDetailResponse, selection: ProductSelection) {
  const variant = selectedProductVariant(product, selection.productVariantId);
  const inputs = selection.textInputs ?? [];
  let configurationValid = product.type === "MADE_TO_ORDER"
    ? Boolean(variant?.active)
    : !selection.productVariantId && inputs.length === 0;
  const options: ProductOptionSnapshotResponse[] = [];
  for (const group of product.optionGroups) {
    const selected = variant?.selections.find((candidate) => candidate.groupKey === group.key);
    const selectedValue = group.values.find((candidate) => candidate.key === selected?.valueKey);
    const value = group.type === "SELECT"
      ? selectedValue?.name
      : inputs.find((input) => input.groupKey === group.key)?.value?.trim();
    if ((group.required && !value)
      || (group.type === "SELECT" && selected && !selectedValue)
      || (group.type === "TEXT" && value && value.length > (group.inputMaxLength ?? 200))) {
      configurationValid = false;
    }
    if (value) options.push({
      type: group.type, groupName: group.name, value,
      priceAdjustment: group.type === "TEXT" ? group.inputPriceAdjustment ?? 0 : 0,
      sortOrder: group.sortOrder,
    });
  }
  if (inputs.some((input) => !product.optionGroups.some((group) => group.type === "TEXT" && group.key === input.groupKey))
    || variant?.selections.some((selected) => !product.optionGroups.some((group) => group.type === "SELECT" && group.key === selected.groupKey))) {
    configurationValid = false;
  }
  options.sort((left, right) => left.sortOrder - right.sortOrder);
  const variantPriceAdjustment = variant?.priceAdjustment ?? 0;
  const textOptionPriceAdjustment = options.reduce((sum, option) => sum + option.priceAdjustment, 0);
  return {
    productVariantId: variant?.id ?? selection.productVariantId ?? null,
    options,
    label: options.map((option) => `${option.groupName}: ${option.value}`).join(" / ") || "기본 조합",
    variantPriceAdjustment, textOptionPriceAdjustment,
    unitPrice: product.price + variantPriceAdjustment + textOptionPriceAdjustment,
    configurationValid,
  };
}
