import { parseApiDateTime } from "@/shared/lib";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import type { GuestRecordRecoveryResponse } from "./api";

export function saveGuestRecordRecovery(result: GuestRecordRecoveryResponse) {
  try {
    sessionStorage.setItem(SESSION_KEYS.guestRecordRecovery, JSON.stringify(result));
  } catch {
    // 세션 저장소를 사용할 수 없어도 현재 화면의 복구 결과는 계속 사용할 수 있다.
  }
}

export function loadGuestRecordRecovery(): GuestRecordRecoveryResponse | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEYS.guestRecordRecovery);
    if (!raw) return null;

    const result = JSON.parse(raw) as Partial<GuestRecordRecoveryResponse>;
    if (
      typeof result.accessToken !== "string"
      || typeof result.expiresAt !== "string"
      || !Array.isArray(result.orders)
      || !Array.isArray(result.bookings)
      || parseApiDateTime(result.expiresAt) <= Date.now()
    ) {
      clearGuestRecordRecovery();
      return null;
    }
    return result as GuestRecordRecoveryResponse;
  } catch {
    clearGuestRecordRecovery();
    return null;
  }
}

export function clearGuestRecordRecovery() {
  try {
    sessionStorage.removeItem(SESSION_KEYS.guestRecordRecovery);
  } catch {
    // 세션 저장소를 사용할 수 없는 환경에서는 정리할 값도 없다.
  }
}
