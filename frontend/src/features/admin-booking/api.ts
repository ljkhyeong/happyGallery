import { adminHeaders as h, api } from "@/shared/api";
import type { AdminBookingResponse, BookingNoShowResponse } from "@/shared/types";

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

export function markNoShow(
  adminKey: string,
  bookingId: number,
): Promise<BookingNoShowResponse> {
  return api<BookingNoShowResponse>(`/admin/bookings/${bookingId}/no-show`, {
    method: "POST",
    headers: h(adminKey),
  });
}
