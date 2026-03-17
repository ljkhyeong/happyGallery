import { api } from "@/shared/api";
import type { BookingDetailResponse, RescheduleResponse, CancelResponse } from "@/shared/types";

export function fetchBooking(bookingId: number, token: string): Promise<BookingDetailResponse> {
  return api<BookingDetailResponse>(`/bookings/${bookingId}`, {
    headers: { "X-Access-Token": token },
  });
}

export function rescheduleBooking(
  bookingId: number,
  newSlotId: number,
  token: string,
): Promise<RescheduleResponse> {
  return api<RescheduleResponse>(`/bookings/${bookingId}/reschedule`, {
    method: "PATCH",
    headers: { "X-Access-Token": token },
    body: { newSlotId },
  });
}

export function cancelBooking(bookingId: number, token: string): Promise<CancelResponse> {
  return api<CancelResponse>(`/bookings/${bookingId}`, {
    method: "DELETE",
    headers: { "X-Access-Token": token },
  });
}
