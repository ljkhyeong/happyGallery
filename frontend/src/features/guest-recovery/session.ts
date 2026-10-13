import { parseApiDateTime } from "@/shared/lib";
import {
  ownedSessionValue,
  readOwnedSessionValue,
  removeOwnedSessionValue,
  type CustomerSessionStorageHandle,
} from "@/shared/storage/customerSessionOwner";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";
import type { CustomerSessionSnapshot } from "@/shared/api";
import type { GuestRecordRecoveryResponse } from "./api";

export type GuestRecordRecoverySession =
  CustomerSessionStorageHandle<GuestRecordRecoveryResponse>;

function isGuestRecordRecovery(value: unknown): value is GuestRecordRecoveryResponse {
  const result = value as Partial<GuestRecordRecoveryResponse> | undefined;
  return Boolean(result)
    && typeof result?.accessToken === "string"
    && result.accessToken.length > 0
    && typeof result.expiresAt === "string";
}

function sameGuestRecordRecovery(
  left: GuestRecordRecoveryResponse,
  right: GuestRecordRecoveryResponse,
): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}

export function saveGuestRecordRecovery(
  result: GuestRecordRecoveryResponse,
  expectedSnapshot?: CustomerSessionSnapshot,
): GuestRecordRecoverySession | null {
  const recovery = ownedSessionValue(result, expectedSnapshot);
  if (!recovery) return null;
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
  const recovery = readOwnedSessionValue(
    SESSION_KEYS.guestRecordRecovery,
    isGuestRecordRecovery,
    expectedSnapshot,
  );
  if (!recovery) return null;
  const expiresAt = parseApiDateTime(recovery.value.expiresAt);
  if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    clearGuestRecordRecovery(recovery);
    return null;
  }
  return recovery;
}

export function clearGuestRecordRecovery(
  expected?: GuestRecordRecoverySession,
): void {
  const recovery = expected ?? loadGuestRecordRecovery();
  if (!recovery) return;
  removeOwnedSessionValue(
    SESSION_KEYS.guestRecordRecovery,
    isGuestRecordRecovery,
    recovery,
    sameGuestRecordRecovery,
  );
}
