import { parseApiDateTime } from "@/shared/lib";
import {
  currentCustomerSessionStorageOwner,
  isCurrentCustomerSessionStorageOwner,
  isCustomerSessionStorageOwner,
  sameCustomerSessionStorageOwner,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import type { CustomerSessionSnapshot } from "@/shared/api";
import type { GuestRecordRecoveryResponse } from "./api";

export type GuestRecordRecoverySession =
  CustomerSessionStorageHandle<GuestRecordRecoveryResponse>;

function readGuestRecordRecovery(
  raw: string,
): GuestRecordRecoverySession | null {
  const parsed = JSON.parse(raw) as Partial<CustomerSessionStorageHandle<unknown>>;
  const result = parsed.value as Partial<GuestRecordRecoveryResponse> | undefined;
  if (
    !isCustomerSessionStorageOwner(parsed.owner)
    || !result
    || typeof result.accessToken !== "string"
    || result.accessToken.length === 0
    || typeof result.expiresAt !== "string"
    || !Array.isArray(result.orders)
    || !Array.isArray(result.bookings)
  ) {
    return null;
  }
  return {
    owner: parsed.owner,
    value: result as GuestRecordRecoveryResponse,
  };
}

function sameGuestRecordRecovery(
  left: GuestRecordRecoverySession,
  right: GuestRecordRecoverySession,
): boolean {
  return sameCustomerSessionStorageOwner(left.owner, right.owner)
    && JSON.stringify(left.value) === JSON.stringify(right.value);
}

export function saveGuestRecordRecovery(
  result: GuestRecordRecoveryResponse,
  expectedSnapshot?: CustomerSessionSnapshot,
): GuestRecordRecoverySession | null {
  const owner = currentCustomerSessionStorageOwner(expectedSnapshot);
  if (!owner) return null;
  const recovery = { owner, value: result };
  try {
    sessionStorage.setItem(
      SESSION_KEYS.guestRecordRecovery,
      JSON.stringify(recovery),
    );
  } catch {
    // 세션 저장소를 사용할 수 없어도 현재 화면의 복구 결과는 계속 사용할 수 있다.
  }
  return recovery;
}

export function loadGuestRecordRecovery(
  expectedSnapshot?: CustomerSessionSnapshot,
): GuestRecordRecoverySession | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEYS.guestRecordRecovery);
    if (!raw) return null;

    const recovery = readGuestRecordRecovery(raw);
    if (
      !recovery
      || !isCurrentCustomerSessionStorageOwner(
        recovery.owner,
        expectedSnapshot,
      )
    ) {
      return null;
    }
    const expiresAt = parseApiDateTime(recovery.value.expiresAt);
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      clearGuestRecordRecovery(recovery);
      return null;
    }
    return recovery;
  } catch {
    return null;
  }
}

export function clearGuestRecordRecovery(
  expected?: GuestRecordRecoverySession,
): void {
  try {
    const recovery = expected ?? loadGuestRecordRecovery();
    if (!recovery) return;
    const raw = sessionStorage.getItem(SESSION_KEYS.guestRecordRecovery);
    if (!raw) return;
    const stored = readGuestRecordRecovery(raw);
    if (!stored || !sameGuestRecordRecovery(stored, recovery)) return;
    sessionStorage.removeItem(SESSION_KEYS.guestRecordRecovery);
  } catch {
    // 세션 저장소를 사용할 수 없는 환경에서는 정리할 값도 없다.
  }
}
