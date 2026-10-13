import type {
  BookingPayload,
  OrderPayload as GeneratedOrderPayload,
  PassPayload,
  PreparePaymentRequest,
  ShippingAddress as GeneratedShippingAddress,
} from "@/generated/api/payment";
import type { PaymentStatusResponse } from "@/generated/api/paymentQuery";

export type PaymentContext = PreparePaymentRequest["context"];

export type FulfillmentType = GeneratedOrderPayload["fulfillmentType"];

export type ShippingAddress = GeneratedShippingAddress & {
  addressLine2: string | null;
};

export type OrderPayload = GeneratedOrderPayload & {
  madeToOrderConsentVersion: string | null;
  shippingAddress: ShippingAddress | null;
};

export type PaymentPayload = OrderPayload | BookingPayload | PassPayload;
export type CustomerPaymentStatus = PaymentStatusResponse["status"];
