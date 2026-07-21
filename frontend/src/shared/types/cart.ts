import type { ProductType } from "./product";

export interface CartItemResponse {
  productId: number;
  productName: string;
  price: number;
  qty: number;
  subtotal: number;
  available: boolean;
  productType: ProductType;
}

export interface CartResponse {
  items: CartItemResponse[];
  totalAmount: number;
}
