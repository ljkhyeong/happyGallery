import { sendGuestBookingVerification as requestVerification } from "@/generated/api/booking";
import { api } from "@/shared/api";
import type {
  ClassResponse,
  PublicSlotResponse,
  SendVerificationResponse,
} from "@/shared/types";

export type PhoneVerificationPurpose =
  | "SIGNUP"
  | "PASSWORD_RESET"
  | "MEMBER_PHONE_REGISTRATION"
  | "MEMBER_PHONE_CHANGE"
  | "GUEST_BOOKING"
  | "GUEST_ORDER"
  | "GUEST_CLAIM"
  | "GUEST_RECORD_RECOVERY"
  | "GUEST_PAYMENT_STATUS_RECOVERY";

interface SendVerificationRequest {
  phone: string;
  purpose: PhoneVerificationPurpose;
}

export function fetchClasses(): Promise<ClassResponse[]> {
  return api<ClassResponse[]>("/classes");
}

export function fetchUpcomingSlots(classId: number, days = 14): Promise<PublicSlotResponse[]> {
  return api<PublicSlotResponse[]>("/slots/upcoming", { params: { classId, days } });
}

export function sendVerification(body: SendVerificationRequest): Promise<SendVerificationResponse> {
  return requestVerification(body);
}
