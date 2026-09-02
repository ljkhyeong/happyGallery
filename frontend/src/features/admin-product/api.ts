import {
  adjustInventory as adjustAdminInventory,
  changeStatus,
  deleteSmartStoreInventoryMapping,
  getSmartStoreInventoryMapping,
  listSmartStoreInventoryMappingHistory,
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
  applySmartStoreProductNotice,
  createSmartStoreProductNotice,
  deleteSmartStoreProductNotice,
  getSmartStoreProductNotice,
  listSmartStoreInspectionProducts,
  listSmartStoreProductNotices,
  restoreSmartStoreInspectionProduct,
  updateSmartStoreProductNotice,
  type AdjustInventoryRequest,
  type CreateProductRequest,
  type InventoryAdjustmentResponse,
  type ProductResponse,
  type SaveSmartStoreInventoryMappingRequest,
  type SmartStoreInventoryMappingResponse,
  type SmartStoreInventoryMappingHistoryResponse,
  type SmartStoreChannelProductResponse,
  type SmartStoreProductCatalogPageResponse,
  type SmartStoreProductPreviewResponse,
  type SaveSmartStoreNoticeRequest,
  type SmartStoreInspectionPageResponse,
  type SmartStoreNoticePageResponse,
  type SmartStoreNoticeResponse,
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

export function fetchSmartStoreInventoryMappingHistory(
  adminKey: string,
  productId: number,
): Promise<SmartStoreInventoryMappingHistoryResponse[]> {
  return listSmartStoreInventoryMappingHistory(productId, {
    headers: adminHeaders(adminKey),
  });
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
  expectedMappingVersion: number,
  previousOriginConfirmed: boolean,
): Promise<void> {
  return deleteSmartStoreInventoryMapping(
    productId,
    { expectedMappingVersion, previousOriginConfirmed },
    { headers: adminHeaders(adminKey) },
  );
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
  previewVersion: string,
): Promise<void> {
  return applySmartStoreProductSync(
    productId,
    { previewVersion },
    { headers: adminHeaders(adminKey) },
  );
}

export function fetchSmartStoreInspections(
  adminKey: string,
): Promise<SmartStoreInspectionPageResponse> {
  return listSmartStoreInspectionProducts(
    { page: 1, size: 100 },
    { headers: adminHeaders(adminKey) },
  );
}

export function requestSmartStoreInspectionRestore(
  adminKey: string,
  channelProductNo: number,
): Promise<void> {
  return restoreSmartStoreInspectionProduct(channelProductNo, {
    headers: adminHeaders(adminKey),
  });
}

export function fetchSmartStoreNotices(
  adminKey: string,
): Promise<SmartStoreNoticePageResponse> {
  return listSmartStoreProductNotices(
    { page: 1, size: 100 },
    { headers: adminHeaders(adminKey) },
  );
}

export function fetchSmartStoreNotice(
  adminKey: string,
  sellerNoticeId: number,
): Promise<SmartStoreNoticeResponse> {
  return getSmartStoreProductNotice(sellerNoticeId, {
    headers: adminHeaders(adminKey),
  });
}

export function saveSmartStoreNotice(
  adminKey: string,
  sellerNoticeId: number | null,
  request: SaveSmartStoreNoticeRequest,
) {
  const options = { headers: adminHeaders(adminKey) };
  return sellerNoticeId === null
    ? createSmartStoreProductNotice(request, options)
    : updateSmartStoreProductNotice(sellerNoticeId, request, options);
}

export function removeSmartStoreNotice(
  adminKey: string,
  sellerNoticeId: number,
): Promise<void> {
  return deleteSmartStoreProductNotice(sellerNoticeId, {
    headers: adminHeaders(adminKey),
  });
}

export function applyNoticeToSmartStoreProducts(
  adminKey: string,
  sellerNoticeId: number,
  channelProductNos: number[],
): Promise<void> {
  return applySmartStoreProductNotice(
    sellerNoticeId,
    { channelProductNos },
    { headers: adminHeaders(adminKey) },
  );
}
