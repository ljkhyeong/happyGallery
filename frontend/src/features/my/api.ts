import { api } from "@/shared/api";

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
  purchasedAt: string;
  expiresAt: string;
  totalCredits: number;
  remainingCredits: number;
  totalPrice: number;
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
