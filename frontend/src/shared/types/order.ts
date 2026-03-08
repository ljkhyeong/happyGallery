import type { OrderStatus } from "./admin";

export interface CreateOrderRequest {
  phone: string;
  verificationCode: string;
  name: string;
  items: OrderItemInput[];
}

export interface OrderItemInput {
  productId: number;
  qty: number;
}

export interface OrderResponse {
  orderId: number;
  accessToken: string;
  status: OrderStatus;
  totalAmount: number;
  paidAt: string;
}

export interface OrderDetailResponse {
  orderId: number;
  status: OrderStatus;
  totalAmount: number;
  paidAt: string;
  approvalDeadlineAt: string;
  items: OrderItemDto[];
  fulfillment: FulfillmentDto | null;
}

export interface OrderItemDto {
  productId: number;
  qty: number;
  unitPrice: number;
}

export type FulfillmentType = "SHIPPING" | "PICKUP";

export interface FulfillmentDto {
  type: FulfillmentType;
  status: OrderStatus;
  expectedShipDate: string | null;
  pickupDeadlineAt: string | null;
}
