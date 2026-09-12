export function productDetailHref(productId: number, productVariantId?: number | null) {
  const path = `/products/${productId}`;
  return productVariantId == null ? path : `${path}?variantId=${productVariantId}`;
}
