import {
  claimGuestRecords as requestGuestClaim,
  previewGuestClaims,
  verifyPhoneAndPreviewGuestClaims,
} from "@/generated/api/customerStore";

export function getGuestClaimPreview() {
  return previewGuestClaims();
}

export function verifyGuestClaimPhone(verificationCode: string) {
  return verifyPhoneAndPreviewGuestClaims({ verificationCode });
}

export function claimGuestRecords(orderIds: number[], bookingIds: number[]) {
  return requestGuestClaim({ orderIds, bookingIds });
}
