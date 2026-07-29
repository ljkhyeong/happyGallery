import {
  cancelGuestBooking,
  getGuestBooking,
  listAvailableSlots,
  rescheduleGuestBooking,
  type BookingDetailResponse,
  type CancelResponse,
  type PublicSlotResponse,
  type RescheduleResponse,
} from "@/generated/api/booking";

export function fetchBooking(bookingId: number, token: string): Promise<BookingDetailResponse> {
  return getGuestBooking(bookingId, {
    headers: { "X-Access-Token": token },
  });
}

export function rescheduleBooking(
  bookingId: number,
  newSlotId: number,
  token: string,
): Promise<RescheduleResponse> {
  return rescheduleGuestBooking(bookingId, { newSlotId }, {
    headers: { "X-Access-Token": token },
  });
}

export function fetchRescheduleSlots(
  classId: number,
  date: string,
): Promise<PublicSlotResponse[]> {
  return listAvailableSlots({ classId, date });
}

export function cancelBooking(bookingId: number, token: string): Promise<CancelResponse> {
  return cancelGuestBooking(bookingId, {
    headers: { "X-Access-Token": token },
  });
}
