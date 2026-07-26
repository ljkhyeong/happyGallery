import type {
  ListProductsParams,
  ListProductsSort,
  ProductDetailResponse as GeneratedProductDetailResponse,
  ProductDetailResponseType,
} from "@/generated/api/product";

export type ProductType = ProductDetailResponseType;
export type ProductStatus = "ACTIVE" | "INACTIVE";
export type ProductSortOrder = ListProductsSort;
export type ProductDetailResponse = GeneratedProductDetailResponse & {
  specification: string | null;
  careInstructions: string | null;
  productionLeadDays: number | null;
};

export interface ProductResponse {
  id: number;
  name: string;
  type: ProductType;
  category: string | null;
  price: number;
  description: string | null;
  imageUrl: string | null;
  specification: string | null;
  careInstructions: string | null;
  productionLeadDays: number | null;
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
  description?: string;
  imageUrl?: string;
  specification?: string;
  careInstructions?: string;
  productionLeadDays?: number;
}

export interface UpdateProductRequest {
  name: string;
  category?: string;
  price: number;
  description?: string;
  imageUrl?: string;
  specification?: string;
  careInstructions?: string;
  productionLeadDays?: number;
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

export type ProductFilterParams = ListProductsParams;
