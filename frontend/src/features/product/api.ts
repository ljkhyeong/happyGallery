import { api } from "@/shared/api";
import type { ProductDetailResponse, ProductFilterParams } from "@/shared/types";

export function fetchProducts(filters?: ProductFilterParams): Promise<ProductDetailResponse[]> {
  return api<ProductDetailResponse[]>("/products", {
    params: filters as Record<string, string | undefined>,
  });
}

export function fetchProduct(id: number): Promise<ProductDetailResponse> {
  return api<ProductDetailResponse>(`/products/${id}`);
}

export function fetchCategories(): Promise<string[]> {
  return api<string[]>("/products/categories");
}
