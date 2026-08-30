import {
  claimGuestRecords as requestGuestClaim,
  previewGuestClaims,
  verifyPhoneAndPreviewGuestClaims,
  type GuestClaimPreviewResponse,
  type GuestClaimResultResponse,
} from "@/generated/api/customerStore";

export type GuestClaimPreview = GuestClaimPreviewResponse;
export type GuestClaimResult = GuestClaimResultResponse;

export function getGuestClaimPreview() {
  return previewGuestClaims();
}

export function verifyGuestClaimPhone(verificationCode: string) {
  return verifyPhoneAndPreviewGuestClaims({ verificationCode });
}

export function claimGuestRecords(orderIds: number[], bookingIds: number[]) {
  return requestGuestClaim({ orderIds, bookingIds });
}
