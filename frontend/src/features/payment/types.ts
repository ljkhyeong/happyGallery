export type PaymentContext = "ORDER" | "BOOKING" | "PASS";
export type DepositPaymentMethod = "CARD" | "EASY_PAY";

export interface OrderItemRef {
  productId: number;
  qty: number;
}

export interface OrderPayload {
  type: "ORDER";
  userId?: number | null;
  phone?: string | null;
  verificationCode?: string | null;
  name?: string | null;
  items: OrderItemRef[];
}

export interface BookingPayload {
  type: "BOOKING";
  userId?: number | null;
  phone?: string | null;
  verificationCode?: string | null;
  name?: string | null;
  slotId: number;
  passId?: number | null;
  paymentMethod?: DepositPaymentMethod | null;
}

export interface PassPayload {
  type: "PASS";
  userId: number;
}

export type PaymentPayload = OrderPayload | BookingPayload | PassPayload;

export interface PreparePaymentResponse {
  orderId: string;
  amount: number;
  context: PaymentContext;
}

export interface ConfirmPaymentResponse {
  context: PaymentContext;
  domainId: number;
  accessToken: string | null;
}
