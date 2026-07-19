import type { OrderStatus } from "./admin";
import type { RefundProgress } from "./refund";

export interface OrderItemInput {
  productId: number;
  qty: number;
}

export interface OrderDetailResponse {
  orderId: number;
  status: OrderStatus;
  totalAmount: number;
  paidAt: string;
  approvalDeadlineAt: string;
  items: OrderItemDto[];
  fulfillment: FulfillmentDto | null;
  refund: RefundProgress | null;
}

export interface OrderItemDto {
  productId: number;
  qty: number;
  unitPrice: number;
}

export type FulfillmentType = "SHIPPING" | "PICKUP";

export interface FulfillmentDto {
  type: FulfillmentType;
  expectedShipDate: string | null;
  pickupDeadlineAt: string | null;
}
