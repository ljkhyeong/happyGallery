import {
  cancelGuestVacancyAlert as requestGuestVacancyAlertCancellation,
  cancelMyVacancyAlert as requestMyVacancyAlertCancellation,
  listPublicClasses,
  listMyVacancyAlerts as requestMyVacancyAlerts,
  listUpcomingSlots,
  registerGuestVacancyAlert as requestGuestVacancyAlert,
  registerMyVacancyAlert as requestMyVacancyAlert,
  sendGuestBookingVerification as requestVerification,
  type ClassResponse,
  type GuestVacancyAlertRequest,
  type PublicSlotResponse,
  type SendVerificationRequest,
  type SendVerificationRequestPurpose,
  type SendVerificationResponse,
  type VacancyAlertResponse,
} from "@/generated/api/booking";

export type PhoneVerificationPurpose = SendVerificationRequestPurpose;

export function fetchClasses(): Promise<ClassResponse[]> {
  return listPublicClasses();
}

export function fetchUpcomingSlots(classId: number, days = 14): Promise<PublicSlotResponse[]> {
  return listUpcomingSlots({ classId, days, includeFull: true });
}

export function sendVerification(body: SendVerificationRequest): Promise<SendVerificationResponse> {
  return requestVerification(body);
}

export function registerGuestVacancyAlert(
  slotId: number,
  body: GuestVacancyAlertRequest,
): Promise<VacancyAlertResponse> {
  return requestGuestVacancyAlert(slotId, body);
}

export function cancelGuestVacancyAlert(slotId: number, accessToken: string): Promise<void> {
  return requestGuestVacancyAlertCancellation(slotId, {
    headers: { "X-Access-Token": accessToken },
  });
}

export function registerMyVacancyAlert(slotId: number): Promise<VacancyAlertResponse> {
  return requestMyVacancyAlert(slotId);
}

export function fetchMyVacancyAlerts(): Promise<VacancyAlertResponse[]> {
  return requestMyVacancyAlerts();
}

export function cancelMyVacancyAlert(slotId: number): Promise<void> {
  return requestMyVacancyAlertCancellation(slotId);
}
