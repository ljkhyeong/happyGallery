import type { ProductTextInputRequest } from "@/generated/api/customerStore";

export function productOptionLineKey(
  productId: number,
  variantId: number | null | undefined,
  inputs: readonly ProductTextInputRequest[] = [],
) {
  return JSON.stringify([productId, variantId ?? null, [...inputs]
    .sort((left, right) => left.groupKey.localeCompare(right.groupKey))
    .map((input) => [input.groupKey, input.value ?? ""])]);
}
