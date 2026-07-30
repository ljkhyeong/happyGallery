import type {
  BookingPayload as GeneratedBookingPayload,
  ConfirmPaymentResponse as GeneratedConfirmPaymentResponse,
  OrderItemRef as GeneratedOrderItemRef,
  OrderPayload as GeneratedOrderPayload,
  PassPayload as GeneratedPassPayload,
  PreparePaymentRequest as GeneratedPreparePaymentRequest,
  PreparePaymentResponse as GeneratedPreparePaymentResponse,
  ShippingAddress as GeneratedShippingAddress,
} from "@/generated/api/payment";
import type {
  PassPaymentPolicyResponse as GeneratedPassPaymentPolicyResponse,
  PaymentStatusResponse as GeneratedPaymentStatusResponse,
} from "@/generated/api/paymentQuery";

export type PaymentContext = GeneratedPreparePaymentRequest["context"];
export type DepositPaymentMethod = NonNullable<GeneratedBookingPayload["paymentMethod"]>;

export type OrderItemRef = GeneratedOrderItemRef;
export type FulfillmentType = GeneratedOrderPayload["fulfillmentType"];

export type ShippingAddress = GeneratedShippingAddress & {
  addressLine2: string | null;
};

export type OrderPayload = GeneratedOrderPayload & {
  madeToOrderConsentVersion: string | null;
  shippingAddress: ShippingAddress | null;
};

export type BookingPayload = GeneratedBookingPayload;
export type PassPayload = GeneratedPassPayload;
export type PaymentPayload = OrderPayload | BookingPayload | PassPayload;
export type PreparePaymentResponse = GeneratedPreparePaymentResponse;
export type ConfirmPaymentResponse = GeneratedConfirmPaymentResponse;
export type PaymentStatusResponse = GeneratedPaymentStatusResponse;
export type CustomerPaymentStatus = PaymentStatusResponse["status"];
export type PassPaymentPolicyResponse = GeneratedPassPaymentPolicyResponse;
