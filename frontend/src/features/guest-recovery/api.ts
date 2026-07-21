import { api } from "@/shared/api";

export interface RecoveredGuestOrder {
  orderId: number;
  status: string;
  totalAmount: number;
  createdAt: string;
}

export interface RecoveredGuestBooking {
  bookingId: number;
  status: string;
  className: string;
  startAt: string;
  endAt: string;
}

export interface GuestRecordRecoveryResponse {
  accessToken: string;
  expiresAt: string;
  orders: RecoveredGuestOrder[];
  bookings: RecoveredGuestBooking[];
}

export function recoverGuestRecords(
  phone: string,
  verificationCode: string,
): Promise<GuestRecordRecoveryResponse> {
  return api<GuestRecordRecoveryResponse>("/guest-records/recovery", {
    method: "POST",
    body: { phone, verificationCode },
  });
}
