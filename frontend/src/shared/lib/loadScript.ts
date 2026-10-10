interface LoadGlobalScriptOptions {
  timeoutMs: number;
  errorMessage: string;
}

const pendingScripts = new Map<string, Promise<unknown>>();

/**
 * 외부 SDK 스크립트를 한 번만 불러와 전역 값을 돌려준다.
 * 이미 전역 값이 있으면 바로 쓰고, 시간 초과·다운로드 실패·전역 값 누락이면 script를 지우고 실패한다.
 * 실패한 요청은 기억하지 않으므로 다음 호출에서 다시 불러온다.
 */
export function loadGlobalScript<T>(
  src: string,
  readGlobal: () => T | undefined,
  { timeoutMs, errorMessage }: LoadGlobalScriptOptions,
): Promise<T> {
  const loaded = readGlobal();
  if (loaded !== undefined) return Promise.resolve(loaded);
  const pending = pendingScripts.get(src) as Promise<T> | undefined;
  if (pending) return pending;

  const request = new Promise<T>((resolve, reject) => {
    const script = document.createElement("script");
    const cleanup = () => {
      window.clearTimeout(timeout);
      script.onload = null;
      script.onerror = null;
    };
    const fail = () => {
      cleanup();
      script.remove();
      reject(new Error(errorMessage));
    };
    const timeout = window.setTimeout(fail, timeoutMs);
    script.src = src;
    script.async = true;
    script.onload = () => {
      const value = readGlobal();
      if (value === undefined) {
        fail();
        return;
      }
      cleanup();
      resolve(value);
    };
    script.onerror = fail;
    document.head.appendChild(script);
  }).catch((error: unknown) => {
    pendingScripts.delete(src);
    throw error;
  });
  pendingScripts.set(src, request);
  return request;
}
