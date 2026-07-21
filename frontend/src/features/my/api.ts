import { api } from "@/shared/api";
import type { MemberPassRefundResponse, RefundProgress } from "@/shared/types";

export interface MyOrderSummary {
  orderId: number;
  status: string;
  totalAmount: number;
  paidAt: string;
  createdAt: string;
}

export interface MyBookingSummary {
  bookingId: number;
  status: string;
  className: string;
  startAt: string;
  endAt: string;
  depositAmount: number;
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
  return api<MyBookingSummary[]>("/me/bookings");
}

export function fetchMyPasses() {
  return api<MyPassSummary[]>("/me/passes");
}

export function refundMyPass(passId: number) {
  return api<MemberPassRefundResponse>(`/me/passes/${passId}/refund`, { method: "POST" });
}
