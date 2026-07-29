import { listMyBookings as requestMyBookings } from "@/generated/api/booking";
import {
  listMyOrders,
  listMyPasses,
  refundMyPass as requestMyPassRefund,
} from "@/generated/api/customerStore";

export type { MyBookingSummary } from "@/generated/api/booking";
export type {
  MyOrderSummary,
  MyPassSummary,
} from "@/generated/api/customerStore";

export function fetchMyOrders() {
  return listMyOrders();
}

export function fetchMyBookings() {
  return requestMyBookings();
}

export function fetchMyPasses() {
  return listMyPasses();
}

export function refundMyPass(passId: number) {
  return requestMyPassRefund(passId);
}
