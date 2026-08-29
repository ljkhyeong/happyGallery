import {
  adjustInventory as adjustAdminInventory,
  changeStatus,
  deleteSmartStoreInventoryMapping,
  getSmartStoreInventoryMapping,
  getSmartStoreProduct,
  listAll,
  listInventoryAdjustments,
  listSmartStoreProducts,
  register,
  retrySmartStoreInventorySync,
  saveSmartStoreInventoryMapping,
  updateAdminProduct,
  previewSmartStoreProductSync,
  applySmartStoreProductSync,
  type AdjustInventoryRequest,
  type CreateProductRequest,
  type InventoryAdjustmentResponse,
  type ProductResponse,
  type SaveSmartStoreInventoryMappingRequest,
  type SmartStoreInventoryMappingResponse,
  type SmartStoreChannelProductResponse,
  type SmartStoreProductCatalogPageResponse,
  type SmartStoreProductPreviewResponse,
  type UpdateProductRequest,
  type UpdateProductStatusRequestStatus,
} from "@/generated/api/adminCatalog";
import { adminHeaders, ApiError } from "@/shared/api";

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

export async function fetchSmartStoreInventoryMapping(
  adminKey: string,
  productId: number,
): Promise<SmartStoreInventoryMappingResponse | null> {
  try {
    return await getSmartStoreInventoryMapping(productId, { headers: adminHeaders(adminKey) });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) return null;
    throw error;
  }
}

export function fetchSmartStoreProducts(
  adminKey: string,
  page: number,
): Promise<SmartStoreProductCatalogPageResponse> {
  return listSmartStoreProducts(
    { page, size: 100 },
    { headers: adminHeaders(adminKey) },
  );
}

export function fetchSmartStoreProduct(
  adminKey: string,
  originProductNo: number,
): Promise<SmartStoreChannelProductResponse> {
  return getSmartStoreProduct(originProductNo, {
    headers: adminHeaders(adminKey),
  });
}

export function saveSmartStoreMapping(
  adminKey: string,
  productId: number,
  body: SaveSmartStoreInventoryMappingRequest,
): Promise<SmartStoreInventoryMappingResponse> {
  return saveSmartStoreInventoryMapping(
    productId,
    body,
    { headers: adminHeaders(adminKey) },
  );
}

export function retrySmartStoreSync(
  adminKey: string,
  productId: number,
): Promise<SmartStoreInventoryMappingResponse> {
  return retrySmartStoreInventorySync(productId, { headers: adminHeaders(adminKey) });
}

export function removeSmartStoreMapping(
  adminKey: string,
  productId: number,
): Promise<void> {
  return deleteSmartStoreInventoryMapping(productId, { headers: adminHeaders(adminKey) });
}

export function fetchSmartStoreProductPreview(
  adminKey: string,
  productId: number,
): Promise<SmartStoreProductPreviewResponse> {
  return previewSmartStoreProductSync(productId, { headers: adminHeaders(adminKey) });
}

export function applySmartStoreProduct(
  adminKey: string,
  productId: number,
  productVersion: number,
): Promise<void> {
  return applySmartStoreProductSync(
    productId,
    { productVersion },
    { headers: adminHeaders(adminKey) },
  );
}
