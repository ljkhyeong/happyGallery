import { adminHeaders as h, api } from "@/shared/api";
import type {
  AdminBookingResponse,
  AdminBookingCancelRequest,
  AdminBookingCancelResponse,
  BookingNoShowResponse,
  BookingSettlementResponse,
} from "@/shared/types";

export function fetchBookings(
  adminKey: string,
  date: string,
  status?: string,
): Promise<AdminBookingResponse[]> {
  return api<AdminBookingResponse[]>("/admin/bookings", {
    headers: h(adminKey),
    params: { date, status },
  });
}

export function cancelBookingByAdmin(
  adminKey: string,
  bookingId: number,
  body: AdminBookingCancelRequest,
): Promise<AdminBookingCancelResponse> {
  return api<AdminBookingCancelResponse>(`/admin/bookings/${bookingId}/cancel`, {
    method: "POST",
    headers: h(adminKey),
    body,
  });
}

export function markNoShow(
  adminKey: string,
  bookingId: number,
): Promise<BookingNoShowResponse> {
  return api<BookingNoShowResponse>(`/admin/bookings/${bookingId}/no-show`, {
    method: "POST",
    headers: h(adminKey),
  });
}

export function markBalancePaid(
  adminKey: string,
  bookingId: number,
): Promise<BookingSettlementResponse> {
  return api<BookingSettlementResponse>(`/admin/bookings/${bookingId}/balance-payment`, {
    method: "POST",
    headers: h(adminKey),
  });
}

export function updateArrears(
  adminKey: string,
  bookingId: number,
  arrears: boolean,
): Promise<BookingSettlementResponse> {
  return api<BookingSettlementResponse>(`/admin/bookings/${bookingId}/arrears`, {
    method: "PUT",
    headers: h(adminKey),
    body: { arrears },
  });
}

export function completeBooking(
  adminKey: string,
  bookingId: number,
): Promise<BookingSettlementResponse> {
  return api<BookingSettlementResponse>(`/admin/bookings/${bookingId}/complete`, {
    method: "POST",
    headers: h(adminKey),
  });
}
