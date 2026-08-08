import {
  listRecoveredGuestBookings as listRecoveredGuestBookingsGenerated,
  listRecoveredGuestOrders as listRecoveredGuestOrdersGenerated,
  recoverGuestRecords as recoverGuestRecordsGenerated,
  type GuestRecoveredBookingPageResponse,
  type GuestRecoveredOrderPageResponse,
} from "@/generated/api/guestRecordRecovery";

export type GuestRecordRecoveryResponse = Awaited<
  ReturnType<typeof recoverGuestRecordsGenerated>
>;

export function recoverGuestRecords(
  phone: string,
  verificationCode: string,
): Promise<GuestRecordRecoveryResponse> {
  return recoverGuestRecordsGenerated({ phone, verificationCode });
}

export function fetchRecoveredGuestOrders(
  accessToken: string,
  cursor?: string,
  signal?: AbortSignal,
): Promise<GuestRecoveredOrderPageResponse> {
  return listRecoveredGuestOrdersGenerated(
    { cursor, size: 20 },
    {
      signal,
      headers: { "X-Access-Token": accessToken },
    },
  );
}

export function fetchRecoveredGuestBookings(
  accessToken: string,
  cursor?: string,
  signal?: AbortSignal,
): Promise<GuestRecoveredBookingPageResponse> {
  return listRecoveredGuestBookingsGenerated(
    { cursor, size: 20 },
    {
      signal,
      headers: { "X-Access-Token": accessToken },
    },
  );
}
