export function sumQuantitiesByVariant(
  lines: readonly { productVariantId: number; qty: number }[],
): Map<number, number> {
  const quantities = new Map<number, number>();
  for (const line of lines) {
    quantities.set(line.productVariantId, (quantities.get(line.productVariantId) ?? 0) + line.qty);
  }
  return quantities;
}
