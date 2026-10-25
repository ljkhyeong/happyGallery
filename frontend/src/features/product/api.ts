import {
  getProduct,
  listProductCategories,
  listProducts,
  type ListProductsParams,
  type ProductDetailResponse,
} from "@/generated/api/product";

export function fetchProducts(filters?: ListProductsParams): Promise<ProductDetailResponse[]> {
  return listProducts(filters);
}

export function fetchProduct(id: number): Promise<ProductDetailResponse> {
  return getProduct(id);
}

export function fetchCategories(): Promise<string[]> {
  return listProductCategories();
}
