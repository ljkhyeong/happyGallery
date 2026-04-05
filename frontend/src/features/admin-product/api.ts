import { adminHeaders as h, api } from "@/shared/api";
import type { ProductResponse, CreateProductRequest } from "@/shared/types";

export function fetchProducts(adminKey: string): Promise<ProductResponse[]> {
  return api<ProductResponse[]>("/admin/products", {
    headers: h(adminKey),
  });
}

export function createProduct(adminKey: string, body: CreateProductRequest): Promise<ProductResponse> {
  return api<ProductResponse>("/admin/products", {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}
