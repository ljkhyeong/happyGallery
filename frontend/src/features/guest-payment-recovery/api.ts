import {
  recoverGuestPaymentStatuses as recoverGuestPaymentStatusesGenerated,
} from "@/generated/api/guestRecordRecovery";

export type GuestPaymentStatusRecoveryResponse = Awaited<
  ReturnType<typeof recoverGuestPaymentStatusesGenerated>
>;

export function recoverGuestPaymentStatuses(
  phone: string,
  verificationCode: string,
): Promise<GuestPaymentStatusRecoveryResponse> {
  return recoverGuestPaymentStatusesGenerated({ phone, verificationCode });
}
