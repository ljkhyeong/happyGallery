import type { VacancyAlertResponse } from "@/generated/api/booking";
import type { CustomerSessionSnapshot } from "@/shared/api";
import {
  currentCustomerSessionStorageOwner,
  readOwnedSessionValue,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";

type GuestVacancyAlertSession = CustomerSessionStorageHandle<VacancyAlertResponse[]>;

function isGuestVacancyAlert(value: unknown): value is VacancyAlertResponse {
  if (!value || typeof value !== "object") return false;
  const alert = value as Partial<VacancyAlertResponse>;
  return Number.isSafeInteger(alert.alertId)
    && Number(alert.alertId) > 0
    && Number.isSafeInteger(alert.slotId)
    && Number(alert.slotId) > 0
    && alert.status === "WAITING"
    && typeof alert.accessToken === "string"
    && alert.accessToken.length > 0;
}

function isGuestVacancyAlertList(value: unknown): value is VacancyAlertResponse[] {
  return Array.isArray(value) && value.every(isGuestVacancyAlert);
}

function loadGuestVacancyAlerts(
  expectedSnapshot?: CustomerSessionSnapshot,
): GuestVacancyAlertSession | null {
  return readOwnedSessionValue(SESSION_KEYS.guestVacancyAlerts, isGuestVacancyAlertList, expectedSnapshot);
}

export function findGuestVacancyAlert(
  slotId: number,
  expectedSnapshot?: CustomerSessionSnapshot,
): VacancyAlertResponse | null {
  return loadGuestVacancyAlerts(expectedSnapshot)?.value
    .find((alert) => alert.slotId === slotId) ?? null;
}

export function saveGuestVacancyAlert(
  alert: VacancyAlertResponse,
  expectedSnapshot: CustomerSessionSnapshot,
): VacancyAlertResponse | null {
  if (!isGuestVacancyAlert(alert)) return null;
  const owner = currentCustomerSessionStorageOwner(expectedSnapshot);
  if (!owner) return null;
  const existing = loadGuestVacancyAlerts(expectedSnapshot)?.value ?? [];
  const session: GuestVacancyAlertSession = {
    owner,
    value: [...existing.filter((item) => item.slotId !== alert.slotId), alert],
  };
  try {
    sessionStorage.setItem(SESSION_KEYS.guestVacancyAlerts, JSON.stringify(session));
  } catch {
    // 세션 저장소를 사용할 수 없어도 현재 화면의 신청 상태는 유지한다.
  }
  return alert;
}

export function clearGuestVacancyAlert(
  expectedAlert: VacancyAlertResponse,
  expectedSnapshot: CustomerSessionSnapshot,
): boolean {
  const session = loadGuestVacancyAlerts(expectedSnapshot);
  if (!session) return false;
  const stored = session.value.find((alert) => alert.slotId === expectedAlert.slotId);
  if (!stored || stored.accessToken !== expectedAlert.accessToken) return false;
  const remaining = session.value.filter((alert) => alert.slotId !== expectedAlert.slotId);
  try {
    if (remaining.length === 0) {
      sessionStorage.removeItem(SESSION_KEYS.guestVacancyAlerts);
    } else {
      sessionStorage.setItem(
        SESSION_KEYS.guestVacancyAlerts,
        JSON.stringify({ owner: session.owner, value: remaining }),
      );
    }
  } catch {
    // 세션 저장소를 사용할 수 없으면 현재 화면 상태만 정리한다.
  }
  return true;
}
