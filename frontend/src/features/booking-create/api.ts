import { api } from "@/shared/api";
import type {
  ClassResponse,
  PublicSlotResponse,
  SendVerificationRequest,
  SendVerificationResponse,
} from "@/shared/types";

export function fetchClasses(): Promise<ClassResponse[]> {
  return api<ClassResponse[]>("/classes");
}

export function fetchUpcomingSlots(classId: number, days = 14): Promise<PublicSlotResponse[]> {
  return api<PublicSlotResponse[]>("/slots/upcoming", { params: { classId, days } });
}

export function sendVerification(body: SendVerificationRequest): Promise<SendVerificationResponse> {
  return api<SendVerificationResponse>("/bookings/phone-verifications", {
    method: "POST",
    body,
  });
}
