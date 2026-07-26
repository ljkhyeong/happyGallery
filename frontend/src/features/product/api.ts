import {
  getProduct,
  listProductCategories,
  listProducts,
  type ListProductsParams,
} from "@/generated/api/product";
import type { ProductDetailResponse } from "@/shared/types/product";

export function fetchProducts(filters?: ListProductsParams): Promise<ProductDetailResponse[]> {
  return listProducts(filters) as Promise<ProductDetailResponse[]>;
}

export function fetchProduct(id: number): Promise<ProductDetailResponse> {
  return getProduct(id) as Promise<ProductDetailResponse>;
}

export function fetchCategories(): Promise<string[]> {
  return listProductCategories();
}
