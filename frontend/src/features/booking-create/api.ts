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

export function fetchAvailableSlots(classId: number, date: string): Promise<PublicSlotResponse[]> {
  return api<PublicSlotResponse[]>("/slots", { params: { classId, date } });
}

export function sendVerification(body: SendVerificationRequest): Promise<SendVerificationResponse> {
  return api<SendVerificationResponse>("/bookings/phone-verifications", {
    method: "POST",
    body,
  });
}
