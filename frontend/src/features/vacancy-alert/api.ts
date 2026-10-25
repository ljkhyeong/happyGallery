import {
  cancelGuestVacancyAlert as requestGuestVacancyAlertCancellation,
  cancelMyVacancyAlert as requestMyVacancyAlertCancellation,
  listMyVacancyAlerts as requestMyVacancyAlerts,
  registerGuestVacancyAlert as requestGuestVacancyAlert,
  registerMyVacancyAlert as requestMyVacancyAlert,
  type GuestVacancyAlertRequest,
  type VacancyAlertResponse,
} from "@/generated/api/booking";

import { queryKeys, runForCurrentCustomer } from "@/shared/api";

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

export function cancelMyVacancyAlert(slotId: number): Promise<void> {
  return requestMyVacancyAlertCancellation(slotId);
}

/** 회원 빈자리 알림 목록. 예약 화면의 신청 버튼과 내 정보 목록이 같은 캐시를 쓴다. */
export function myVacancyAlertsQuery() {
  return {
    queryKey: queryKeys.member.vacancyAlerts,
    queryFn: ({ signal }: { signal: AbortSignal }) =>
      runForCurrentCustomer(() => requestMyVacancyAlerts({ signal })),
  };
}
