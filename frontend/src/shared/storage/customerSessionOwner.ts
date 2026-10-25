import {
  captureCustomerSession,
  isCurrentCustomerSession,
  type CustomerSessionSnapshot,
} from "@/shared/api/customerSession";
import { parseApiDateTime } from "@/shared/lib";

export interface CustomerSessionStorageOwner {
  boundaryEpoch: string | null;
  boundaryCustomerId: number | null;
}

export interface CustomerSessionStorageHandle<T> {
  owner: CustomerSessionStorageOwner;
  value: T;
}

export function currentCustomerSessionStorageOwner(
  expectedSnapshot: CustomerSessionSnapshot = captureCustomerSession(),
): CustomerSessionStorageOwner | null {
  if (!isCurrentCustomerSession(expectedSnapshot)) return null;
  return {
    boundaryEpoch: expectedSnapshot.boundaryEpoch,
    boundaryCustomerId: expectedSnapshot.boundaryCustomerId,
  };
}

export function isCurrentCustomerSessionStorageOwner(
  owner: CustomerSessionStorageOwner,
  expectedSnapshot?: CustomerSessionSnapshot,
): boolean {
  const currentOwner = currentCustomerSessionStorageOwner(expectedSnapshot);
  return currentOwner !== null && sameCustomerSessionStorageOwner(owner, currentOwner);
}

export function isCustomerSessionStorageOwner(
  value: unknown,
): value is CustomerSessionStorageOwner {
  if (typeof value !== "object" || value === null) return false;
  const owner = value as Partial<CustomerSessionStorageOwner>;
  return (
    owner.boundaryEpoch === null
    || (
      typeof owner.boundaryEpoch === "string"
      && owner.boundaryEpoch.length > 0
    )
  ) && (
    owner.boundaryCustomerId === null
    || (
      typeof owner.boundaryCustomerId === "number"
      && Number.isSafeInteger(owner.boundaryCustomerId)
      && owner.boundaryCustomerId > 0
    )
  );
}

export function sameCustomerSessionStorageOwner(
  left: CustomerSessionStorageOwner,
  right: CustomerSessionStorageOwner,
): boolean {
  return left.boundaryEpoch === right.boundaryEpoch
    && left.boundaryCustomerId === right.boundaryCustomerId;
}

/** 현재 세션(또는 기대한 snapshot)을 소유자로 한 저장 봉투. 세션이 바뀌었으면 null. */
export function ownedSessionValue<T>(
  value: T,
  expectedSnapshot?: CustomerSessionSnapshot,
): CustomerSessionStorageHandle<T> | null {
  const owner = currentCustomerSessionStorageOwner(expectedSnapshot);
  return owner ? { owner, value } : null;
}

/** 저장된 JSON을 소유자 봉투로 읽는다. 형식이 다르면 null이고, JSON 오류는 호출자가 처리한다. */
export function parseOwnedSessionValue<T>(
  raw: string,
  isValue: (value: unknown) => value is T,
): CustomerSessionStorageHandle<T> | null {
  const parsed = JSON.parse(raw) as Partial<CustomerSessionStorageHandle<unknown>>;
  if (!isCustomerSessionStorageOwner(parsed.owner) || !isValue(parsed.value)) {
    return null;
  }
  return { owner: parsed.owner, value: parsed.value };
}

/** 현재 세션 소유자가 저장한 값만 읽는다. 저장소를 쓸 수 없거나 형식이 다르면 null. */
export function readOwnedSessionValue<T>(
  key: string,
  isValue: (value: unknown) => value is T,
  expectedSnapshot?: CustomerSessionSnapshot,
): CustomerSessionStorageHandle<T> | null {
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return null;
    const stored = parseOwnedSessionValue(raw, isValue);
    return stored && isCurrentCustomerSessionStorageOwner(stored.owner, expectedSnapshot)
      ? stored
      : null;
  } catch {
    return null;
  }
}

/** 저장된 소유자와 값이 기대한 봉투와 같을 때만 지운다. 다른 흐름이 새로 저장한 값은 남긴다. */
export function removeOwnedSessionValue<T>(
  key: string,
  isValue: (value: unknown) => value is T,
  expected: CustomerSessionStorageHandle<T>,
  sameValue: (left: T, right: T) => boolean,
): void {
  try {
    const raw = sessionStorage.getItem(key);
    if (!raw) return;
    const stored = parseOwnedSessionValue(raw, isValue);
    if (
      !stored
      || !sameCustomerSessionStorageOwner(stored.owner, expected.owner)
      || !sameValue(stored.value, expected.value)
    ) {
      return;
    }
    sessionStorage.removeItem(key);
  } catch {
    // 세션 저장소를 사용할 수 없으면 정리할 값도 없다.
  }
}

export interface OwnedRecoverySessionStoreOptions<T extends { expiresAt: string }> {
  key: string;
  isValue: (value: unknown) => value is T;
  /** 봉투를 sessionStorage에 쓰기 전에 함께 저장할 값을 처리한다(예: 주문별 상태 토큰). */
  beforeStore?: (
    recovery: CustomerSessionStorageHandle<T>,
    expectedSnapshot?: CustomerSessionSnapshot,
  ) => void;
  /** 봉투를 지운 뒤 함께 정리할 값을 처리한다. */
  afterClear?: (recovery: CustomerSessionStorageHandle<T>) => void;
}

export interface OwnedRecoverySessionStore<T> {
  /** 현재 세션(또는 기대한 snapshot) 소유로 저장한다. 세션이 바뀌었으면 저장하지 않고 null. */
  save: (
    result: T,
    expectedSnapshot?: CustomerSessionSnapshot,
  ) => CustomerSessionStorageHandle<T> | null;
  /** 현재 세션 소유자가 저장한 값만 읽는다. 만료된 값은 지우고 null. */
  load: (expectedSnapshot?: CustomerSessionSnapshot) => CustomerSessionStorageHandle<T> | null;
  /** 저장된 소유자·값이 기대한 봉투(없으면 지금 읽은 값)와 같을 때만 지운다. */
  clear: (expected?: CustomerSessionStorageHandle<T>) => void;
}

function sameSerializedValue<T>(left: T, right: T): boolean {
  return JSON.stringify(left) === JSON.stringify(right);
}

/**
 * 휴대폰 인증으로 받은 만료 시각이 있는 복구 결과를 현재 회원 세션 소유로 sessionStorage에 보관한다.
 * 저장 실패는 삼키고 현재 화면의 결과는 계속 사용한다.
 */
export function createOwnedRecoverySessionStore<T extends { expiresAt: string }>({
  key,
  isValue,
  beforeStore,
  afterClear,
}: OwnedRecoverySessionStoreOptions<T>): OwnedRecoverySessionStore<T> {
  function save(
    result: T,
    expectedSnapshot?: CustomerSessionSnapshot,
  ): CustomerSessionStorageHandle<T> | null {
    const recovery = ownedSessionValue(result, expectedSnapshot);
    if (!recovery) return null;
    beforeStore?.(recovery, expectedSnapshot);
    try {
      sessionStorage.setItem(key, JSON.stringify(recovery));
    } catch {
      // 세션 저장소를 사용할 수 없어도 현재 화면의 복구 결과는 계속 사용할 수 있다.
    }
    return recovery;
  }

  function load(
    expectedSnapshot?: CustomerSessionSnapshot,
  ): CustomerSessionStorageHandle<T> | null {
    const recovery = readOwnedSessionValue(key, isValue, expectedSnapshot);
    if (!recovery) return null;
    const expiresAt = parseApiDateTime(recovery.value.expiresAt);
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
      clear(recovery);
      return null;
    }
    return recovery;
  }

  function clear(expected?: CustomerSessionStorageHandle<T>): void {
    const recovery = expected ?? load();
    if (!recovery) return;
    removeOwnedSessionValue(key, isValue, recovery, sameSerializedValue);
    afterClear?.(recovery);
  }

  return { save, load, clear };
}
