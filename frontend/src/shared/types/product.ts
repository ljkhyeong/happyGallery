export type ProductType = "READY_STOCK" | "MADE_TO_ORDER";
export type ProductStatus = "ACTIVE" | "INACTIVE";
export type ProductSortOrder = "newest" | "price_asc" | "price_desc";

export interface ProductDetailResponse {
  id: number;
  name: string;
  type: ProductType;
  category: string | null;
  price: number;
  available: boolean;
}

export interface ProductResponse {
  id: number;
  name: string;
  type: ProductType;
  category: string | null;
  price: number;
  status: ProductStatus;
  available: boolean;
  quantity: number;
}

export interface CreateProductRequest {
  name: string;
  type: ProductType;
  category?: string;
  price: number;
  quantity: number;
}

export type InventoryAdjustmentType = "INCREASE" | "DECREASE";

export interface AdjustInventoryRequest {
  type: InventoryAdjustmentType;
  quantity: number;
  reason: string;
}

export interface InventoryAdjustmentResponse {
  id: number;
  productId: number;
  type: InventoryAdjustmentType;
  quantity: number;
  quantityBefore: number;
  quantityAfter: number;
  reason: string;
  adjustedByAdminId: number | null;
  adjustedBy: string;
  adjustedAt: string;
}

export interface ProductFilterParams {
  type?: ProductType;
  category?: string;
  keyword?: string;
  sort?: ProductSortOrder;
}
