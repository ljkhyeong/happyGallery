export type ProductType = "READY_STOCK" | "MADE_TO_ORDER";
export type ProductStatus = "ACTIVE" | "INACTIVE";

export interface ProductDetailResponse {
  id: number;
  name: string;
  type: ProductType;
  price: number;
  available: boolean;
}

export interface ProductResponse {
  id: number;
  name: string;
  type: ProductType;
  price: number;
  status: ProductStatus;
  available: boolean;
  quantity: number;
}

export interface CreateProductRequest {
  name: string;
  type: ProductType;
  price: number;
  quantity: number;
}
