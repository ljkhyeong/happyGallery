import {
  recoverGuestRecords as recoverGuestRecordsGenerated,
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
