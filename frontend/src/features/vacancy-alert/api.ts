import {
  cancelGuestVacancyAlert as requestGuestVacancyAlertCancellation,
  cancelMyVacancyAlert as requestMyVacancyAlertCancellation,
  listMyVacancyAlerts as requestMyVacancyAlerts,
  registerGuestVacancyAlert as requestGuestVacancyAlert,
  registerMyVacancyAlert as requestMyVacancyAlert,
  type GuestVacancyAlertRequest,
  type VacancyAlertResponse,
} from "@/generated/api/booking";

export type { VacancyAlertResponse };

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

export function fetchMyVacancyAlerts(signal?: AbortSignal): Promise<VacancyAlertResponse[]> {
  return requestMyVacancyAlerts({ signal });
}

export function cancelMyVacancyAlert(slotId: number): Promise<void> {
  return requestMyVacancyAlertCancellation(slotId);
}
