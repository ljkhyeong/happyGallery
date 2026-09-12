import {
  listPublicClasses,
  listUpcomingSlots,
  sendGuestBookingVerification as requestVerification,
  type ClassResponse,
  type PublicSlotResponse,
  type SendVerificationRequest,
  type SendVerificationRequestPurpose,
  type SendVerificationResponse,
} from "@/generated/api/booking";

export type PhoneVerificationPurpose = SendVerificationRequestPurpose;

export function fetchClasses(): Promise<ClassResponse[]> {
  return listPublicClasses();
}

export function fetchUpcomingSlots(classId: number, days = 14): Promise<PublicSlotResponse[]> {
  return listUpcomingSlots({ classId, days, includeFull: true });
}

export function sendVerification(body: SendVerificationRequest, botToken?: string): Promise<SendVerificationResponse> {
  return requestVerification(body, { headers: botToken ? { "X-Bot-Token": botToken } : undefined });
}
