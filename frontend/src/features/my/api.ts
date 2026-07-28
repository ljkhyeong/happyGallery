import { listMyBookings as requestMyBookings } from "@/generated/api/booking";
import { api } from "@/shared/api";
import type { MemberPassRefundResponse, OrderStatus, RefundProgress } from "@/shared/types";

export type { MyBookingSummary } from "@/generated/api/booking";

export interface MyOrderSummary {
  orderId: number;
  status: OrderStatus;
  totalAmount: number;
  paidAt: string | null;
  createdAt: string;
}

export interface MyPassSummary {
  passId: number;
  planCode: "LEGACY_ALL_CLASSES" | "REGULAR_CRAFT_8";
  planName: string;
  purchasedAt: string;
  expiresAt: string;
  totalCredits: number;
  remainingCredits: number;
  totalPrice: number;
  refund: RefundProgress | null;
}

export function fetchMyOrders() {
  return api<MyOrderSummary[]>("/me/orders");
}

export function fetchMyBookings() {
  return requestMyBookings();
}

export function fetchMyPasses() {
  return api<MyPassSummary[]>("/me/passes");
}

export function refundMyPass(passId: number) {
  return api<MemberPassRefundResponse>(`/me/passes/${passId}/refund`, { method: "POST" });
}
