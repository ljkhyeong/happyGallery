import { api } from "@/shared/api";
import type { CancelResponse, MyBookingDetailResponse } from "@/shared/types";

export function fetchMyBooking(bookingId: number): Promise<MyBookingDetailResponse> {
  return api<MyBookingDetailResponse>(`/me/bookings/${bookingId}`);
}

export function rescheduleMyBooking(bookingId: number, newSlotId: number) {
  return api(`/me/bookings/${bookingId}/reschedule`, {
    method: "PATCH",
    body: { newSlotId },
  });
}

export function cancelMyBooking(bookingId: number): Promise<CancelResponse> {
  return api<CancelResponse>(`/me/bookings/${bookingId}`, {
    method: "DELETE",
  });
}
