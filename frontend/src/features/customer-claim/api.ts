import { api } from "@/shared/api";

export interface ClaimOrderSummary {
  orderId: number;
  status: string;
  totalAmount: number;
  createdAt: string;
}

export interface ClaimBookingSummary {
  bookingId: number;
  status: string;
  className: string;
  startAt: string;
  endAt: string;
}

export interface ClaimPassSummary {
  passId: number;
  remainingCredits: number;
  totalCredits: number;
  expiresAt: string;
  totalPrice: number;
}

export interface GuestClaimPreview {
  phoneVerified: boolean;
  orders: ClaimOrderSummary[];
  bookings: ClaimBookingSummary[];
  passes: ClaimPassSummary[];
}

export interface GuestClaimResult {
  claimedOrderCount: number;
  claimedBookingCount: number;
  claimedPassCount: number;
}

export function getGuestClaimPreview() {
  return api<GuestClaimPreview>("/me/guest-claims/preview");
}

export function verifyGuestClaimPhone(verificationCode: string) {
  return api<GuestClaimPreview>("/me/guest-claims/verify", {
    method: "POST",
    body: { verificationCode },
  });
}

export function claimGuestRecords(orderIds: number[], bookingIds: number[], passIds: number[]) {
  return api<GuestClaimResult>("/me/guest-claims", {
    method: "POST",
    body: { orderIds, bookingIds, passIds },
  });
}
