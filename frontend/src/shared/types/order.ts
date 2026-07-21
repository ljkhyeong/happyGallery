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
  shippingFee: number;
  paidAt: string;
  approvalDeadlineAt: string;
  items: OrderItemDto[];
  fulfillment: FulfillmentDto | null;
  refund: RefundProgress | null;
}

export interface OrderPricePolicyResponse {
  shippingFee: number;
  madeToOrderConsentVersion: string;
  madeToOrderConsentText: string;
}

export type OrderDelayDecision = "ACCEPT" | "REJECT";

export interface OrderDelayResponseRequest {
  decision: OrderDelayDecision;
}

export interface OrderCustomerActionResponse {
  orderId: number;
  status: OrderStatus;
  refund: RefundProgress | null;
}

export interface OrderItemDto {
  productId: number;
  productName: string;
  qty: number;
  unitPrice: number;
}

export type FulfillmentType = "SHIPPING" | "PICKUP";

export interface FulfillmentDto {
  type: FulfillmentType;
  expectedShipDate: string | null;
  pickupDeadlineAt: string | null;
  carrier: string | null;
  trackingNumber: string | null;
}
