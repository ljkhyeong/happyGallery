import { adminHeaders as h, api } from "@/shared/api";
import type {
  AdjustInventoryRequest,
  CreateProductRequest,
  InventoryAdjustmentResponse,
  ProductResponse,
  ProductStatus,
} from "@/shared/types";

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

export function updateProductStatus(
  adminKey: string,
  productId: number,
  status: ProductStatus,
): Promise<ProductResponse> {
  return api<ProductResponse>(`/admin/products/${productId}/status`, {
    method: "PATCH",
    headers: h(adminKey),
    body: { status },
  });
}

export function adjustInventory(
  adminKey: string,
  productId: number,
  body: AdjustInventoryRequest,
): Promise<InventoryAdjustmentResponse> {
  return api<InventoryAdjustmentResponse>(`/admin/products/${productId}/inventory-adjustments`, {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}

export function fetchInventoryAdjustments(
  adminKey: string,
  productId: number,
): Promise<InventoryAdjustmentResponse[]> {
  return api<InventoryAdjustmentResponse[]>(`/admin/products/${productId}/inventory-adjustments`, {
    headers: h(adminKey),
  });
}
