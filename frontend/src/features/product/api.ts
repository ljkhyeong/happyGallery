import { api } from "@/shared/api";
import type { ProductDetailResponse } from "@/shared/types";

export function fetchProducts(): Promise<ProductDetailResponse[]> {
  return api<ProductDetailResponse[]>("/products");
}

export function fetchProduct(id: number): Promise<ProductDetailResponse> {
  return api<ProductDetailResponse>(`/products/${id}`);
}
