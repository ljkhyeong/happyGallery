import { api } from "@/shared/api";
import type { ProductResponse, CreateProductRequest } from "@/shared/types";

export function fetchProducts(adminKey: string): Promise<ProductResponse[]> {
  return api<ProductResponse[]>("/admin/products", {
    headers: { "X-Admin-Key": adminKey },
  });
}

export function createProduct(adminKey: string, body: CreateProductRequest): Promise<ProductResponse> {
  return api<ProductResponse>("/admin/products", {
    method: "POST",
    headers: { "X-Admin-Key": adminKey },
    body,
  });
}
