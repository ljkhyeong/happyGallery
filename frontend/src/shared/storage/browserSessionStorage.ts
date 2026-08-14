function getBrowserSessionStorage(): Storage | null {
  if (typeof window === "undefined") return null;
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

export function readSessionValue(key: string): string | null {
  try {
    return getBrowserSessionStorage()?.getItem(key) ?? null;
  } catch {
    return null;
  }
}

export function writeSessionValue(key: string, value: string): boolean {
  return writeSessionValues([[key, value]]);
}

export function writeSessionValues(
  entries: ReadonlyArray<readonly [key: string, value: string]>,
): boolean {
  const storage = getBrowserSessionStorage();
  if (!storage) return false;
  try {
    for (const [key, value] of entries) {
      storage.setItem(key, value);
    }
    return true;
  } catch {
    for (const [key] of entries) {
      try {
        storage.removeItem(key);
      } catch {
        // 저장소 전체가 차단된 환경에서는 정리도 불가능하다.
      }
    }
    return false;
  }
}

export function removeSessionValues(...keys: string[]): void {
  const storage = getBrowserSessionStorage();
  if (!storage) return;
  for (const key of keys) {
    try {
      storage.removeItem(key);
    } catch {
      // 한 key 정리 실패가 나머지 callback 상태 정리를 막지 않게 한다.
    }
  }
}
