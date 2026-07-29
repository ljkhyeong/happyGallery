import {
  adjustInventory as adjustAdminInventory,
  changeStatus,
  listAll,
  listInventoryAdjustments,
  register,
  updateAdminProduct,
  type AdjustInventoryRequest,
  type CreateProductRequest,
  type InventoryAdjustmentResponse,
  type ProductResponse,
  type UpdateProductRequest,
  type UpdateProductStatusRequestStatus,
} from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";

export type ProductStatus = UpdateProductStatusRequestStatus;

export function fetchProducts(adminKey: string): Promise<ProductResponse[]> {
  return listAll({ headers: adminHeaders(adminKey) });
}

export function createProduct(
  adminKey: string,
  body: CreateProductRequest,
): Promise<ProductResponse> {
  return register(body, { headers: adminHeaders(adminKey) });
}

export function updateProduct(
  adminKey: string,
  productId: number,
  body: UpdateProductRequest,
): Promise<ProductResponse> {
  return updateAdminProduct(productId, body, { headers: adminHeaders(adminKey) });
}

export function updateProductStatus(
  adminKey: string,
  productId: number,
  status: ProductStatus,
): Promise<ProductResponse> {
  return changeStatus(
    productId,
    { status },
    { headers: adminHeaders(adminKey) },
  );
}

export function adjustInventory(
  adminKey: string,
  productId: number,
  body: AdjustInventoryRequest,
): Promise<InventoryAdjustmentResponse> {
  return adjustAdminInventory(productId, body, { headers: adminHeaders(adminKey) });
}

export function fetchInventoryAdjustments(
  adminKey: string,
  productId: number,
): Promise<InventoryAdjustmentResponse[]> {
  return listInventoryAdjustments(productId, { headers: adminHeaders(adminKey) });
}
