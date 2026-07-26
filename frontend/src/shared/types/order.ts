import type { OrderStatus } from "./admin";
import type { ProductType } from "./product";
import type { RefundProgress } from "./refund";

export interface OrderItemInput {
  productId: number;
  qty: number;
}

export interface OrderDetailResponse {
  orderId: number;
  orderNumber: string;
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
  orderItemId: number;
  productId: number;
  productName: string;
  productType: ProductType | null;
  qty: number;
  unitPrice: number;
  specification: string | null;
  careInstructions: string | null;
  productionLeadDays: number | null;
}

export type FulfillmentType = "SHIPPING" | "PICKUP";

export interface FulfillmentDto {
  type: FulfillmentType;
  expectedShipDate: string | null;
  pickupDeadlineAt: string | null;
  carrier: string | null;
  trackingNumber: string | null;
  shippingAddress: {
    recipientName: string;
    phone: string;
    postalCode: string;
    addressLine1: string;
    addressLine2: string | null;
  } | null;
}
