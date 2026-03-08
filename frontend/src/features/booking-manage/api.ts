import { api } from "@/shared/api";
import type { BookingDetailResponse, RescheduleResponse, CancelResponse } from "@/shared/types";

export function fetchBooking(bookingId: number, token: string): Promise<BookingDetailResponse> {
  return api<BookingDetailResponse>(`/bookings/${bookingId}`, {
    params: { token },
  });
}

export function rescheduleBooking(
  bookingId: number,
  newSlotId: number,
  token: string,
): Promise<RescheduleResponse> {
  return api<RescheduleResponse>(`/bookings/${bookingId}/reschedule`, {
    method: "PATCH",
    body: { newSlotId, token },
  });
}

export function cancelBooking(bookingId: number, token: string): Promise<CancelResponse> {
  return api<CancelResponse>(`/bookings/${bookingId}`, {
    method: "DELETE",
    params: { token },
  });
}
