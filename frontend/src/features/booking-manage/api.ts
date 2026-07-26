import {
  cancelGuestBooking,
  getGuestBooking,
  rescheduleGuestBooking,
} from "@/generated/api/booking";
import { api } from "@/shared/api";
import type {
  BookingDetailResponse,
  RescheduleResponse,
  CancelResponse,
  PublicSlotResponse,
} from "@/shared/types";

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
  return api<PublicSlotResponse[]>("/slots", { params: { classId, date } });
}

export function cancelBooking(bookingId: number, token: string): Promise<CancelResponse> {
  return cancelGuestBooking(bookingId, {
    headers: { "X-Access-Token": token },
  });
}
