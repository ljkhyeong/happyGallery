export interface CartItemResponse {
  productId: number;
  productName: string;
  price: number;
  qty: number;
  subtotal: number;
  available: boolean;
}

export interface CartResponse {
  items: CartItemResponse[];
  totalAmount: number;
}
