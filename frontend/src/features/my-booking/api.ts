import {
  cancelMyBooking as requestBookingCancellation,
  getMyBooking,
  reduceMyBookingParticipants as requestParticipantReduction,
  rescheduleMyBooking as requestMyBookingReschedule,
  type ReduceBookingParticipantsResponse,
} from "@/generated/api/booking";
import type { CancelResponse, MyBookingDetailResponse } from "@/shared/types";

export function fetchMyBooking(bookingId: number): Promise<MyBookingDetailResponse> {
  return getMyBooking(bookingId);
}

export function rescheduleMyBooking(bookingId: number, newSlotId: number) {
  return requestMyBookingReschedule(bookingId, { newSlotId });
}

export function cancelMyBooking(bookingId: number): Promise<CancelResponse> {
  return requestBookingCancellation(bookingId);
}

export function reduceMyBookingParticipants(
  bookingId: number,
  participantCount: number,
): Promise<ReduceBookingParticipantsResponse> {
  return requestParticipantReduction(bookingId, { participantCount });
}
